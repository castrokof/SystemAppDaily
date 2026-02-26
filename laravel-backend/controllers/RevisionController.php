<?php

namespace App\Http\Controllers;

use App\OrdenRevision;
use App\ListaParametro;
use App\User;
use App\Models\Admin\Ordenesmtl;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

/**
 * Controller WEB - Revisiones.
 *
 * Flujo real:
 * 1. /revisiones/criticas                 -> Supervisor ve lecturas con Estado=4 (criticas)
 * 2. AJAX adicionarcritica/eliminarcritica -> Marca/desmarca con Coordenada='generar'
 * 3. POST /revisiones/generar             -> Genera ordenes desde las marcadas (Coordenada='generar')
 * 4. /revisiones                          -> Listado de ordenes generadas
 * 5. /revisiones/{id}                     -> Detalle con resultado del wizard
 */
class RevisionController extends Controller
{
    // ========================================
    // LISTADO DE ORDENES DE REVISION
    // ========================================

    /**
     * GET /revisiones
     */
    public function index(Request $request)
    {
        $query = OrdenRevision::with('usuario', 'fotos', 'censoHidraulico');

        if ($request->filled('estado_orden')) {
            $query->where('estado_orden', $request->estado_orden);
        }
        if ($request->filled('usuario_id')) {
            $query->where('usuario_id', $request->usuario_id);
        }
        if ($request->filled('motivo_revision')) {
            $query->where('motivo_revision', $request->motivo_revision);
        }
        if ($request->filled('buscar')) {
            $query->where(function ($q) use ($request) {
                $q->where('codigo_predio', 'LIKE', '%' . $request->buscar . '%')
                  ->orWhere('nombre_suscriptor', 'LIKE', '%' . $request->buscar . '%')
                  ->orWhere('direccion', 'LIKE', '%' . $request->buscar . '%');
            });
        }

        $revisiones = $query->orderBy('created_at', 'desc')->paginate(20);
        $usuarios = User::orderBy('nombre')->pluck('nombre', 'id');

        return view('revisiones.index', compact('revisiones', 'usuarios'));
    }

    // ========================================
    // VER LECTURAS CON CRITICA (supervisor)
    // ========================================

    /**
     * GET /revisiones/criticas
     * Muestra las lecturas de ordenescu con Estado=4 (criticas).
     * Las que tienen Coordenada='generar' estan marcadas para revision.
     */
    public function criticas(Request $request)
    {
        $query = Ordenesmtl::where('Estado', 4);

        // Filtros
        if ($request->filled('critica')) {
            $query->where('Critica', $request->critica);
        }
        if ($request->filled('ciclo')) {
            $query->where('Ciclo', $request->ciclo);
        }
        if ($request->filled('ruta')) {
            $query->where('Ruta', $request->ruta);
        }
        if ($request->filled('marcadas')) {
            if ($request->marcadas == 'si') {
                $query->where('Coordenada', 'generar');
            } elseif ($request->marcadas == 'no') {
                $query->where(function ($q) {
                    $q->whereNull('Coordenada')->orWhere('Coordenada', '!=', 'generar');
                });
            }
        }
        if ($request->filled('buscar')) {
            $query->where(function ($q) use ($request) {
                $q->where('Suscriptor', 'LIKE', '%' . $request->buscar . '%')
                  ->orWhere('Nombre', 'LIKE', '%' . $request->buscar . '%')
                  ->orWhere('Direccion', 'LIKE', '%' . $request->buscar . '%');
            });
        }

        $criticas = $query->orderBy('id', 'desc')->paginate(30);

        // Valores unicos para filtro de critica
        $tiposCritica = Ordenesmtl::where('Estado', 4)
            ->whereNotNull('Critica')
            ->where('Critica', '!=', '')
            ->distinct()
            ->pluck('Critica');

        // Contadores
        $totalCriticas = Ordenesmtl::where('Estado', 4)->count();
        $totalMarcadas = Ordenesmtl::where('Estado', 4)->where('Coordenada', 'generar')->count();

        // Usuarios revisores para asignar
        $revisores = User::orderBy('nombre')->pluck('nombre', 'id');

        return view('revisiones.criticas', compact(
            'criticas', 'tiposCritica', 'revisores', 'totalCriticas', 'totalMarcadas'
        ));
    }

    // ========================================
    // MARCAR / DESMARCAR CRITICAS (AJAX)
    // Usa tu logica existente: Coordenada = 'generar'
    // ========================================

    /**
     * POST /revisiones/adicionar-critica (AJAX)
     * Marca lecturas seleccionadas con Coordenada='generar'
     */
    public function adicionarcritica(Request $request)
    {
        if ($request->ajax()) {
            $id = $request->input('id');

            foreach ($id as $fila) {
                DB::table('ordenescu')
                    ->where([
                        ['id', '=', $fila],
                        ['Estado', '=', 4],
                    ])
                    ->update(['Coordenada' => 'generar']);
            }

            return response()->json(['mensaje' => 'ok']);
        }
    }

    /**
     * POST /revisiones/eliminar-critica (AJAX)
     * Desmarca lecturas seleccionadas (Coordenada = NULL)
     */
    public function eliminarcritica(Request $request)
    {
        if ($request->ajax()) {
            $id = $request->input('id');

            foreach ($id as $fila) {
                DB::table('ordenescu')
                    ->where([
                        ['id', '=', $fila],
                        ['Estado', '=', 4],
                    ])
                    ->update(['Coordenada' => null]);
            }

            return response()->json(['mensaje' => 'ok']);
        }
    }

    // ========================================
    // GENERAR ORDENES DE REVISION
    // ========================================

    /**
     * POST /revisiones/generar
     * Genera ordenes de revision a partir de las lecturas marcadas
     * (Estado=4, Coordenada='generar') que aun no tienen orden.
     */
    public function generar(Request $request)
    {
        $this->validate($request, [
            'usuario_id' => 'required|exists:users,id',
        ]);

        $usuarioId = $request->input('usuario_id');

        // Obtener todas las marcadas que NO tienen orden de revision
        $lecturasConRevision = OrdenRevision::pluck('lectura_id')->toArray();

        $marcadas = Ordenesmtl::where('Estado', 4)
            ->where('Coordenada', 'generar')
            ->when(!empty($lecturasConRevision), function ($q) use ($lecturasConRevision) {
                $q->whereNotIn('id', $lecturasConRevision);
            })
            ->get();

        if ($marcadas->isEmpty()) {
            return redirect()->route('revisiones.criticas')
                ->with('error', 'No hay lecturas marcadas para generar ordenes de revision.');
        }

        $creadas = 0;
        foreach ($marcadas as $lectura) {
            OrdenRevision::crearDesdeLectura($lectura, $usuarioId);
            $creadas++;
        }

        return redirect()->route('revisiones.index')
            ->with('success', "$creadas ordenes de revision creadas y asignadas.");
    }

    // ========================================
    // DETALLE DE UNA REVISION
    // ========================================

    /**
     * GET /revisiones/{id}
     */
    public function show($id)
    {
        $revision = OrdenRevision::with('fotos', 'censoHidraulico', 'usuario', 'lectura')
            ->findOrFail($id);

        return view('revisiones.show', compact('revision'));
    }

    // ========================================
    // ELIMINAR REVISION (solo PENDIENTE)
    // ========================================

    /**
     * DELETE /revisiones/{id}
     */
    public function destroy($id)
    {
        $revision = OrdenRevision::findOrFail($id);

        if ($revision->estado_orden === 'EJECUTADO') {
            return redirect()->back()
                ->with('error', 'No se puede eliminar una revision ya ejecutada.');
        }

        // Devolver la lectura a sin marcar
        if ($revision->lectura_id) {
            DB::table('ordenescu')
                ->where('id', $revision->lectura_id)
                ->update(['Coordenada' => null]);
        }

        $revision->delete();

        return redirect()->route('revisiones.index')
            ->with('success', 'Orden de revision eliminada.');
    }

    // ========================================
    // REASIGNAR USUARIO
    // ========================================

    /**
     * POST /revisiones/{id}/reasignar
     */
    public function reasignar(Request $request, $id)
    {
        $this->validate($request, [
            'usuario_id' => 'required|exists:users,id',
        ]);

        $revision = OrdenRevision::findOrFail($id);

        if ($revision->estado_orden === 'EJECUTADO') {
            return redirect()->back()
                ->with('error', 'No se puede reasignar una revision ya ejecutada.');
        }

        $revision->update(['usuario_id' => $request->usuario_id]);

        return redirect()->route('revisiones.show', $id)
            ->with('success', 'Revision reasignada correctamente.');
    }

    // ========================================
    // GESTION DE LISTAS PARAMETROS
    // ========================================

    /**
     * GET /listas-parametros
     */
    public function listas(Request $request)
    {
        $query = ListaParametro::query();

        if ($request->filled('tipo_lista')) {
            $query->where('tipo_lista', $request->tipo_lista);
        }

        $listas = $query->orderBy('tipo_lista')->orderBy('id')->paginate(50);
        $tipos = ListaParametro::distinct()->pluck('tipo_lista');

        return view('revisiones.listas', compact('listas', 'tipos'));
    }
}
