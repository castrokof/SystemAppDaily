<?php

namespace App\Http\Controllers;

use App\OrdenRevision;
use App\ListaParametro;
use App\User;
use App\Models\Admin\Ordenesmtl;
use Illuminate\Http\Request;

/**
 * Controller WEB - Revisiones.
 *
 * Flujo:
 * 1. /revisiones/criticas           -> Supervisor ve lecturas con critica
 * 2. POST /revisiones/generar       -> Supervisor selecciona y genera ordenes
 * 3. /revisiones                    -> Listado de ordenes generadas
 * 4. /revisiones/{id}               -> Detalle con resultado del wizard
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
     * Muestra las lecturas de ordenescu que tienen Critica != consumo normal
     * y que aun no tienen orden de revision generada.
     */
    public function criticas(Request $request)
    {
        $query = Ordenesmtl::where('Critica', '!=', 'CONSUMO NORMAL')
            ->where('Critica', '!=', '')
            ->whereNotNull('Critica');

        // Excluir las que ya tienen orden de revision
        $lecturasConRevision = OrdenRevision::pluck('lectura_id')->toArray();
        if (!empty($lecturasConRevision)) {
            $query->whereNotIn('id', $lecturasConRevision);
        }

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
        if ($request->filled('buscar')) {
            $query->where(function ($q) use ($request) {
                $q->where('Suscriptor', 'LIKE', '%' . $request->buscar . '%')
                  ->orWhere('Nombre', 'LIKE', '%' . $request->buscar . '%')
                  ->orWhere('Direccion', 'LIKE', '%' . $request->buscar . '%');
            });
        }

        $criticas = $query->orderBy('id', 'desc')->paginate(30);

        // Obtener valores unicos para filtros
        $tiposCritica = Ordenesmtl::where('Critica', '!=', 'CONSUMO NORMAL')
            ->where('Critica', '!=', '')
            ->whereNotNull('Critica')
            ->distinct()
            ->pluck('Critica');

        // Usuarios revisores para asignar
        $revisores = User::orderBy('nombre')->pluck('nombre', 'id');

        return view('revisiones.criticas', compact('criticas', 'tiposCritica', 'revisores'));
    }

    // ========================================
    // GENERAR ORDENES DE REVISION
    // ========================================

    /**
     * POST /revisiones/generar
     * El supervisor selecciona lecturas criticas y genera ordenes de revision.
     */
    public function generar(Request $request)
    {
        $this->validate($request, [
            'lecturas'   => 'required|array|min:1',
            'lecturas.*' => 'exists:ordenescu,id',
            'usuario_id' => 'required|exists:users,id',
        ]);

        $lecturaIds = $request->input('lecturas');
        $usuarioId  = $request->input('usuario_id');
        $creadas    = 0;
        $duplicadas = 0;

        foreach ($lecturaIds as $lecturaId) {
            // Verificar que no exista ya una orden para esta lectura
            $existe = OrdenRevision::where('lectura_id', $lecturaId)->exists();
            if ($existe) {
                $duplicadas++;
                continue;
            }

            $lectura = Ordenesmtl::find($lecturaId);
            if ($lectura) {
                OrdenRevision::crearDesdeLecrura($lectura, $usuarioId);
                $creadas++;
            }
        }

        $msg = "$creadas ordenes de revision creadas.";
        if ($duplicadas > 0) {
            $msg .= " ($duplicadas ya tenian orden asignada)";
        }

        return redirect()->route('revisiones.index')
            ->with('success', $msg);
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
