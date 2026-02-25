<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Macromedidor;
use App\MacroFoto;
use App\User;
use Illuminate\Http\Request;
use Carbon\Carbon;

/**
 * Controller API - Endpoints para la app Android.
 *
 * Endpoints que consume la app:
 *   GET  /api/ordenesMacro?api_token=xxx       -> Descargar ordenes pendientes
 *   POST /api/macromedidoresMovil               -> Subir lectura con fotos
 */
class MacromedidorApiController extends Controller
{
    /**
     * ================================================
     * GET /api/ordenesMacro?api_token=xxx
     * ================================================
     *
     * Descarga las ordenes de macro asignadas al usuario.
     * La app Android espera un array JSON con la estructura de MacroEntity.
     *
     * Response: [
     *   {
     *     "id_orden": 1,
     *     "codigo_macro": "MAC-001",
     *     "ubicacion": "Calle 5 #12-30",
     *     "lectura_anterior": 12500,
     *     "estado": "PENDIENTE",
     *     "lectura_actual": null,
     *     "observacion": null,
     *     "ruta_fotos": null,
     *     "gps_latitud_lectura": null,
     *     "gps_longitud_lectura": null,
     *     "fecha_lectura": null,
     *     "sincronizado": false
     *   },
     *   ...
     * ]
     */
    public function ordenesMacro(Request $request)
    {
        $apiToken = $request->input('api_token');
        if (!$apiToken) {
            return response()->json(['error' => 'api_token requerido'], 401);
        }

        $user = User::where('api_token', $apiToken)->first();
        if (!$user) {
            return response()->json(['error' => 'Token invalido'], 401);
        }

        $macros = Macromedidor::with('fotos')
            ->where('usuario_id', $user->id)
            ->orderBy('created_at', 'desc')
            ->get();

        // Mapear al formato que espera la app (MacroEntity)
        $resultado = $macros->map(function ($macro) {
            return $macro->toApiArray();
        });

        return response()->json($resultado);
    }

    /**
     * ================================================
     * POST /api/macromedidoresMovil
     * ================================================
     *
     * Recibe la lectura desde la app Android.
     * La app envia multipart/form-data con:
     *   - api_token: string
     *   - id_orden: int (= macromedidores.id)
     *   - lectura_actual: string
     *   - observacion: string (nullable)
     *   - gps_latitud: double (nullable)
     *   - gps_longitud: double (nullable)
     *   - fotos[0], fotos[1]...: archivos imagen
     *
     * Response: { "success": true, "message": "...", "id": 1 }
     */
    public function enviarMacro(Request $request)
    {
        // 1. Validar token
        $apiToken = $request->input('api_token');
        if (!$apiToken) {
            return response()->json([
                'success' => false,
                'message' => 'api_token requerido'
            ], 401);
        }

        $user = User::where('api_token', $apiToken)->first();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Token invalido'
            ], 401);
        }

        // 2. Buscar la orden
        $idOrden = $request->input('id_orden');
        $macro = Macromedidor::where('id', $idOrden)
            ->where('usuario_id', $user->id)
            ->first();

        if (!$macro) {
            return response()->json([
                'success' => false,
                'message' => 'Orden no encontrada o no pertenece al usuario'
            ], 404);
        }

        // 3. Actualizar datos de lectura
        $macro->estado                = 'EJECUTADO';
        $macro->lectura_actual        = $request->input('lectura_actual');
        $macro->observacion           = $request->input('observacion');
        $macro->gps_latitud_lectura   = $request->input('gps_latitud');
        $macro->gps_longitud_lectura  = $request->input('gps_longitud');
        $macro->fecha_lectura         = Carbon::now();
        $macro->sincronizado          = true;
        $macro->save();

        // 4. Guardar fotos
        if ($request->hasFile('fotos')) {
            foreach ($request->file('fotos') as $foto) {
                if ($foto->isValid()) {
                    $nombreArchivo = 'macro_' . $macro->id . '_' . time() . '_' . uniqid() . '.' . $foto->getClientOriginalExtension();
                    $ruta = $foto->move(public_path('uploads/macros'), $nombreArchivo);

                    MacroFoto::create([
                        'macromedidor_id' => $macro->id,
                        'ruta_foto'       => 'uploads/macros/' . $nombreArchivo,
                    ]);
                }
            }
        }

        return response()->json([
            'success' => true,
            'message' => 'Lectura recibida correctamente',
            'id'      => $macro->id,
        ]);
    }
}
