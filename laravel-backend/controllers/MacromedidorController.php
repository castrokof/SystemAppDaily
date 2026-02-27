<?php

namespace App\Http\Controllers;

use App\Models\Macromedidor;
use App\Models\Seguridad\Usuario;
use Illuminate\Http\Request;

/**
 * Controller WEB - CRUD de Macromedidores.
 * Para el panel administrativo (formularios Blade).
 */
class MacromedidorController extends Controller
{
    /**
     * Listado de macromedidores con filtros.
     * GET /macromedidores
     */
    public function index(Request $request)
    {
        $query = Macromedidor::with('usuario', 'fotos');

        // Filtro por estado
        if ($request->filled('estado')) {
            $query->where('estado', $request->estado);
        }

        // Filtro por usuario
        if ($request->filled('usuario_id')) {
            $query->where('usuario_id', $request->usuario_id);
        }

        // Buscar por codigo
        if ($request->filled('buscar')) {
            $query->where(function ($q) use ($request) {
                $q->where('codigo_macro', 'LIKE', '%' . $request->buscar . '%')
                  ->orWhere('ubicacion', 'LIKE', '%' . $request->buscar . '%');
            });
        }

        $macros = $query->orderBy('created_at', 'desc')->paginate(20);
        $usuarios = Usuario::orderBy('nombre')->pluck('nombre', 'id');

        return view('macromedidores.index', compact('macros', 'usuarios'));
    }

    /**
     * Formulario de creacion.
     * GET /macromedidores/create
     */
    public function create()
    {
        $usuarios = Usuario::orderBy('nombre')->pluck('nombre', 'id');
        return view('macromedidores.create', compact('usuarios'));
    }

    /**
     * Guardar nuevo macromedidor.
     * POST /macromedidores
     */
    public function store(Request $request)
    {
        $this->validate($request, [
            'codigo_macro'    => 'required|string|unique:macromedidores,codigo_macro',
            'ubicacion'       => 'nullable|string|max:500',
            'lectura_anterior' => 'nullable|integer|min:0',
            'usuario_id'      => 'required|exists:usuario,id',
        ]);

        Macromedidor::create([
            'codigo_macro'     => strtoupper(trim($request->codigo_macro)),
            'ubicacion'        => $request->ubicacion,
            'lectura_anterior' => $request->lectura_anterior ?: 0,
            'estado'           => 'PENDIENTE',
            'usuario_id'       => $request->usuario_id,
        ]);

        return redirect()->route('macromedidores.index')
            ->with('success', 'Macromedidor creado correctamente.');
    }

    /**
     * Detalle de un macromedidor con sus lecturas y fotos.
     * GET /macromedidores/{id}
     */
    public function show($id)
    {
        $macro = Macromedidor::with('fotos', 'usuario')->findOrFail($id);
        return view('macromedidores.show', compact('macro'));
    }

    /**
     * Formulario de edicion.
     * GET /macromedidores/{id}/edit
     */
    public function edit($id)
    {
        $macro = Macromedidor::findOrFail($id);
        $usuarios = Usuario::orderBy('nombre')->pluck('nombre', 'id');
        return view('macromedidores.edit', compact('macro', 'usuarios'));
    }

    /**
     * Actualizar macromedidor.
     * PUT /macromedidores/{id}
     */
    public function update(Request $request, $id)
    {
        $macro = Macromedidor::findOrFail($id);

        $this->validate($request, [
            'codigo_macro'    => 'required|string|unique:macromedidores,codigo_macro,' . $id,
            'ubicacion'       => 'nullable|string|max:500',
            'lectura_anterior' => 'nullable|integer|min:0',
            'usuario_id'      => 'required|exists:usuario,id',
        ]);

        $macro->update([
            'codigo_macro'     => strtoupper(trim($request->codigo_macro)),
            'ubicacion'        => $request->ubicacion,
            'lectura_anterior' => $request->lectura_anterior ?: 0,
            'usuario_id'       => $request->usuario_id,
        ]);

        return redirect()->route('macromedidores.show', $id)
            ->with('success', 'Macromedidor actualizado.');
    }

    /**
     * Eliminar macromedidor.
     * DELETE /macromedidores/{id}
     */
    public function destroy($id)
    {
        $macro = Macromedidor::findOrFail($id);

        // Solo se puede eliminar si esta PENDIENTE
        if ($macro->estado === 'EJECUTADO') {
            return redirect()->back()
                ->with('error', 'No se puede eliminar un macromedidor ya ejecutado.');
        }

        $macro->delete();

        return redirect()->route('macromedidores.index')
            ->with('success', 'Macromedidor eliminado.');
    }

    /**
     * Resetear un macro EJECUTADO a PENDIENTE (re-asignar).
     * POST /macromedidores/{id}/resetear
     */
    public function resetear($id)
    {
        $macro = Macromedidor::findOrFail($id);

        $macro->update([
            'estado'                => 'PENDIENTE',
            'lectura_actual'        => null,
            'observacion'           => null,
            'gps_latitud_lectura'   => null,
            'gps_longitud_lectura'  => null,
            'fecha_lectura'         => null,
            'sincronizado'          => false,
        ]);

        // Eliminar fotos asociadas
        foreach ($macro->fotos as $foto) {
            if (file_exists(public_path($foto->ruta_foto))) {
                unlink(public_path($foto->ruta_foto));
            }
            $foto->delete();
        }

        return redirect()->route('macromedidores.show', $id)
            ->with('success', 'Macromedidor reseteado a PENDIENTE.');
    }
}
