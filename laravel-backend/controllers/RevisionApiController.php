<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\OrdenRevision;
use App\Models\CensoHidraulico;
use App\Models\RevisionFoto;
use App\Models\ListaParametro;
use App\Models\Seguridad\Usuario;
use Illuminate\Http\Request;
use Carbon\Carbon;

/**
 * Controller API - Revisiones.
 *
 * Endpoints que consume la app Android:
 *   GET  /api/ordenesRevision?api_token=xxx     -> Descarga ordenes de revision
 *   POST /api/revisionesMovilV2                  -> Sube revision ejecutada (wizard)
 *   GET  /api/listasParametros?api_token=xxx    -> Descarga listas para dropdowns
 */
class RevisionApiController extends Controller
{
    /**
     * ================================================
     * GET /api/ordenesRevision?api_token=xxx
     * ================================================
     *
     * Descarga las ordenes de revision asignadas al usuario.
     * La app espera un array JSON con la estructura de RevisionEntity.
     */
    public function ordenesRevision(Request $request)
    {
        $apiToken = $request->input('api_token');
        if (!$apiToken) {
            return response()->json(['error' => 'api_token requerido'], 401);
        }

        $user = Usuario::where('api_token', $apiToken)->first();
        if (!$user) {
            return response()->json(['error' => 'Token invalido'], 401);
        }

        $revisiones = OrdenRevision::with('fotos')
            ->where('usuario_id', $user->id)
            ->orderBy('created_at', 'desc')
            ->get();

        $resultado = $revisiones->map(function ($rev) {
            return $rev->toApiArray();
        });

        return response()->json($resultado);
    }

    /**
     * ================================================
     * POST /api/revisionesMovilV2
     * ================================================
     *
     * Recibe la revision ejecutada desde la app Android.
     * Multipart/form-data con todos los campos del wizard:
     *
     * - api_token, id_orden, codigo_predio
     * - estado_acometida, estado_sellos
     * - nombre_atiende, tipo_documento, documento
     * - num_familias, num_personas
     * - motivo_revision, motivo_detalle, generalidades
     * - censo_hidraulico_json: JSON array [{tipo_punto, cantidad, estado}]
     * - gps_latitud, gps_longitud
     * - fotos[0], fotos[1]...: archivos imagen
     * - firma_cliente: archivo imagen PNG
     * - acta_pdf: archivo PDF (nullable)
     */
    public function enviarRevisionV2(Request $request)
    {
        // 1. Validar token
        $apiToken = $request->input('api_token');
        if (!$apiToken) {
            return response()->json([
                'success' => false,
                'message' => 'api_token requerido',
            ], 401);
        }

        $user = Usuario::where('api_token', $apiToken)->first();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Token invalido',
            ], 401);
        }

        // 2. Buscar la orden
        $idOrden = $request->input('id_orden');
        $revision = OrdenRevision::where('id', $idOrden)
            ->where('usuario_id', $user->id)
            ->first();

        if (!$revision) {
            return response()->json([
                'success' => false,
                'message' => 'Orden de revision no encontrada o no pertenece al usuario',
            ], 404);
        }

        // 3. Actualizar datos del wizard
        $revision->estado_orden      = 'EJECUTADO';
        $revision->estado_acometida  = $request->input('estado_acometida');
        $revision->estado_sellos     = $request->input('estado_sellos');
        $revision->nombre_atiende    = $request->input('nombre_atiende');
        $revision->tipo_documento    = $request->input('tipo_documento');
        $revision->documento         = $request->input('documento');
        $revision->num_familias      = $request->input('num_familias');
        $revision->num_personas      = $request->input('num_personas');
        $revision->motivo_revision   = $request->input('motivo_revision');
        $revision->motivo_detalle    = $request->input('motivo_detalle');
        $revision->generalidades     = $request->input('generalidades');
        $revision->gps_latitud_predio  = $request->input('gps_latitud');
        $revision->gps_longitud_predio = $request->input('gps_longitud');
        $revision->fecha_cierre      = Carbon::now();
        $revision->sincronizado      = true;

        // 4. Guardar firma del cliente
        if ($request->hasFile('firma_cliente')) {
            $firma = $request->file('firma_cliente');
            if ($firma->isValid()) {
                $nombreFirma = 'firma_rev_' . $revision->id . '_' . time() . '.' . $firma->getClientOriginalExtension();
                $firma->move(public_path('uploads/revisiones/firmas'), $nombreFirma);
                $revision->firma_cliente = 'uploads/revisiones/firmas/' . $nombreFirma;
            }
        }

        $revision->save();

        // 5. Guardar censo hidraulico
        // Primero eliminar censo anterior (por si es re-envio)
        CensoHidraulico::where('revision_id', $revision->id)->delete();

        $censoJson = $request->input('censo_hidraulico_json');
        if ($censoJson) {
            $censoItems = json_decode($censoJson, true);
            if (is_array($censoItems)) {
                foreach ($censoItems as $item) {
                    CensoHidraulico::create([
                        'revision_id' => $revision->id,
                        'tipo_punto'  => $item['tipo_punto'] ?? '',
                        'cantidad'    => $item['cantidad'] ?? 0,
                        'estado'      => $item['estado'] ?? 'BUENO',
                    ]);
                }
            }
        }

        // 6. Guardar fotos
        if ($request->hasFile('fotos')) {
            foreach ($request->file('fotos') as $foto) {
                if ($foto->isValid()) {
                    $nombreFoto = 'rev_' . $revision->id . '_' . time() . '_' . uniqid() . '.' . $foto->getClientOriginalExtension();
                    $foto->move(public_path('uploads/revisiones/fotos'), $nombreFoto);

                    RevisionFoto::create([
                        'revision_id' => $revision->id,
                        'ruta_foto'   => 'uploads/revisiones/fotos/' . $nombreFoto,
                    ]);
                }
            }
        }

        // 7. Guardar acta PDF (opcional)
        if ($request->hasFile('acta_pdf')) {
            $acta = $request->file('acta_pdf');
            if ($acta->isValid()) {
                $nombreActa = 'acta_rev_' . $revision->id . '_' . time() . '.pdf';
                $acta->move(public_path('uploads/revisiones/actas'), $nombreActa);
                // Puedes guardar la ruta en un campo adicional si lo necesitas
            }
        }

        return response()->json([
            'success' => true,
            'message' => 'Revision recibida correctamente',
            'id'      => $revision->id,
        ]);
    }

    /**
     * ================================================
     * GET /api/listasParametros?api_token=xxx
     * ================================================
     *
     * Descarga las listas de parametros para los dropdowns de la app.
     * La app espera un array JSON con la estructura de ListaEntity.
     */
    public function listasParametros(Request $request)
    {
        $apiToken = $request->input('api_token');
        if (!$apiToken) {
            return response()->json(['error' => 'api_token requerido'], 401);
        }

        $user = Usuario::where('api_token', $apiToken)->first();
        if (!$user) {
            return response()->json(['error' => 'Token invalido'], 401);
        }

        $listas = ListaParametro::activos()->orderBy('tipo_lista')->orderBy('id')->get();

        $resultado = $listas->map(function ($item) {
            return $item->toApiArray();
        });

        return response()->json($resultado);
    }
}
