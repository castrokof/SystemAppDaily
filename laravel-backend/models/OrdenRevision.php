<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use App\Models\Seguridad\Usuario;

class OrdenRevision extends Model
{
    protected $table = 'ordenes_revision';

    protected $fillable = [
        'lectura_id',
        'codigo_predio',
        'nombre_suscriptor',
        'direccion',
        'telefono',
        'ref_medidor',
        'critica_original',
        'lectura_actual',
        'lectura_anterior',
        'consumo_actual',
        'promedio',
        'estado_orden',
        'estado_acometida',
        'estado_sellos',
        'nombre_atiende',
        'tipo_documento',
        'documento',
        'num_familias',
        'num_personas',
        'motivo_revision',
        'motivo_detalle',
        'generalidades',
        'firma_cliente',
        'gps_latitud_predio',
        'gps_longitud_predio',
        'fecha_cierre',
        'sincronizado',
        'usuario_id',
    ];

    protected $casts = [
        'lectura_actual'      => 'integer',
        'lectura_anterior'    => 'integer',
        'consumo_actual'      => 'integer',
        'promedio'            => 'integer',
        'num_familias'        => 'integer',
        'num_personas'        => 'integer',
        'gps_latitud_predio'  => 'double',
        'gps_longitud_predio' => 'double',
        'sincronizado'        => 'boolean',
    ];

    // ========================================
    // RELACIONES
    // ========================================

    public function lectura()
    {
        return $this->belongsTo('App\Models\Admin\Ordenesmtl', 'lectura_id');
    }

    public function usuario()
    {
        return $this->belongsTo(Usuario::class, 'usuario_id');
    }

    public function censoHidraulico()
    {
        return $this->hasMany(CensoHidraulico::class, 'revision_id');
    }

    public function fotos()
    {
        return $this->hasMany(RevisionFoto::class, 'revision_id');
    }

    // ========================================
    // SCOPES
    // ========================================

    public function scopePendientes($query)
    {
        return $query->where('estado_orden', 'PENDIENTE');
    }

    public function scopeEjecutadas($query)
    {
        return $query->where('estado_orden', 'EJECUTADO');
    }

    public function scopeDelUsuario($query, $userId)
    {
        return $query->where('usuario_id', $userId);
    }

    // ========================================
    // HELPERS
    // ========================================

    /**
     * Crear una orden de revision a partir de una lectura critica (ordenescu).
     *
     * Flujo: RevisionController@generar() llama este metodo para cada lectura
     * marcada con Coordenada='generar'.
     *
     * El motivo se determina automaticamente desde el campo Critica de la lectura,
     * con fallback a calculo por consumo/promedio.
     */
    public static function crearDesdeLectura($lectura, $usuarioId, $motivoRevision = null)
    {
        // Determinar motivo automaticamente segun el campo Critica de la lectura
        if (!$motivoRevision) {
            $motivoRevision = self::determinarMotivo($lectura);
        }

        return self::create([
            'lectura_id'         => $lectura->id,
            'codigo_predio'      => $lectura->Suscriptor,
            'nombre_suscriptor'  => trim($lectura->Nombre . ' ' . $lectura->Apell),
            'direccion'          => $lectura->Direccion,
            'telefono'           => $lectura->Telefono,
            'ref_medidor'        => $lectura->Ref_Medidor,
            'critica_original'   => $lectura->Critica,
            'lectura_actual'     => $lectura->Lect_Actual,
            'lectura_anterior'   => $lectura->LA,
            'consumo_actual'     => $lectura->Cons_Act,
            'promedio'           => $lectura->Promedio,
            'estado_orden'       => 'PENDIENTE',
            'motivo_revision'    => $motivoRevision,
            'usuario_id'         => $usuarioId,
        ]);
    }

    /**
     * Determinar motivo de revision desde el campo Critica de la lectura.
     * Primero intenta mapear por texto de critica, luego por calculo numerico.
     */
    public static function determinarMotivo($lectura)
    {
        $critica = strtoupper(trim($lectura->Critica ?? ''));

        // Mapeo por texto de critica (palabras clave comunes)
        $mapeo = [
            'BAJO'      => 'DESVIACION_BAJA',
            'BAJA'      => 'DESVIACION_BAJA',
            'ALTO'      => 'DESVIACION_ALTA',
            'ALTA'      => 'DESVIACION_ALTA',
            'EXCESO'    => 'DESVIACION_ALTA',
            'DETENIDO'  => 'MEDIDOR_DANADO',
            'DANADO'    => 'MEDIDOR_DANADO',
            'DAÑADO'    => 'MEDIDOR_DANADO',
            'AVERIADO'  => 'MEDIDOR_DANADO',
            'FRAUDE'    => 'FRAUDE',
            'IRREGULAR' => 'FRAUDE',
            'DIRECTA'   => 'FRAUDE',
            'IMPEDIDO'  => 'SIN_LECTURA',
            'SIN LECTURA' => 'SIN_LECTURA',
            'CERRADO'   => 'SIN_LECTURA',
        ];

        foreach ($mapeo as $palabra => $motivo) {
            if (strpos($critica, $palabra) !== false) {
                return $motivo;
            }
        }

        // Fallback: calculo por consumo vs promedio
        $consumo = intval($lectura->Cons_Act);
        $promedio = intval($lectura->Promedio);

        if ($consumo < 0 || ($promedio > 0 && $consumo < $promedio * 0.5)) {
            return 'DESVIACION_BAJA';
        } elseif ($promedio > 0 && $consumo > $promedio * 2) {
            return 'DESVIACION_ALTA';
        }

        return 'OTRO';
    }

    /**
     * Formato que espera la app Android (RevisionEntity).
     * Los nombres coinciden con @ColumnInfo del Room Entity.
     */
    public function toApiArray()
    {
        return [
            'id_orden'            => $this->id,
            'codigo_predio'       => $this->codigo_predio,
            'estado_orden'        => $this->estado_orden,
            'estado_acometida'    => $this->estado_acometida,
            'estado_sellos'       => $this->estado_sellos,
            'nombre_atiende'      => $this->nombre_atiende,
            'tipo_documento'      => $this->tipo_documento,
            'documento'           => $this->documento,
            'num_familias'        => $this->num_familias,
            'num_personas'        => $this->num_personas,
            'motivo_revision'     => $this->motivo_revision,
            'motivo_detalle'      => $this->motivo_detalle,
            'generalidades'       => $this->generalidades,
            'firma_cliente'       => $this->firma_cliente,
            'ruta_fotos'          => $this->fotos->pluck('ruta_foto')->implode(','),
            'gps_latitud_predio'  => $this->gps_latitud_predio,
            'gps_longitud_predio' => $this->gps_longitud_predio,
            'fecha_cierre'        => $this->fecha_cierre,
            'sincronizado'        => $this->sincronizado,
        ];
    }
}
