<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use App\Models\Seguridad\Usuario;

class Macromedidor extends Model
{
    protected $table = 'macromedidores';

    protected $fillable = [
        'codigo_macro',
        'ubicacion',
        'lectura_anterior',
        'estado',
        'lectura_actual',
        'observacion',
        'gps_latitud_lectura',
        'gps_longitud_lectura',
        'fecha_lectura',
        'sincronizado',
        'usuario_id',
    ];

    protected $casts = [
        'lectura_anterior'     => 'integer',
        'gps_latitud_lectura'  => 'double',
        'gps_longitud_lectura' => 'double',
        'sincronizado'         => 'boolean',
    ];

    // ========================================
    // RELACIONES
    // ========================================

    public function fotos()
    {
        return $this->hasMany(MacroFoto::class, 'macromedidor_id');
    }

    public function usuario()
    {
        return $this->belongsTo(Usuario::class, 'usuario_id');
    }

    // ========================================
    // SCOPES
    // ========================================

    public function scopePendientes($query)
    {
        return $query->where('estado', 'PENDIENTE');
    }

    public function scopeEjecutados($query)
    {
        return $query->where('estado', 'EJECUTADO');
    }

    public function scopeDelUsuario($query, $userId)
    {
        return $query->where('usuario_id', $userId);
    }

    // ========================================
    // FORMATO PARA API MOVIL
    // ========================================

    /**
     * Formato que espera la app Android (MacroEntity).
     * Los nombres coinciden con @ColumnInfo del Room Entity.
     */
    public function toApiArray()
    {
        return [
            'id_orden'              => $this->id,
            'codigo_macro'          => $this->codigo_macro,
            'ubicacion'             => $this->ubicacion,
            'lectura_anterior'      => $this->lectura_anterior,
            'estado'                => $this->estado,
            'lectura_actual'        => $this->lectura_actual,
            'observacion'           => $this->observacion,
            'ruta_fotos'            => $this->fotos->pluck('ruta_foto')->implode(','),
            'gps_latitud_lectura'   => $this->gps_latitud_lectura,
            'gps_longitud_lectura'  => $this->gps_longitud_lectura,
            'fecha_lectura'         => $this->fecha_lectura,
            'sincronizado'          => $this->sincronizado,
        ];
    }
}
