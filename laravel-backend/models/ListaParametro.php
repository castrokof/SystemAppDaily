<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ListaParametro extends Model
{
    protected $table = 'listas_parametros';

    protected $fillable = [
        'tipo_lista',
        'codigo',
        'descripcion',
        'activo',
    ];

    protected $casts = [
        'activo' => 'boolean',
    ];

    // ========================================
    // SCOPES
    // ========================================

    public function scopeActivos($query)
    {
        return $query->where('activo', true);
    }

    public function scopeTipo($query, $tipoLista)
    {
        return $query->where('tipo_lista', $tipoLista);
    }

    /**
     * Formato que espera la app Android (ListaEntity).
     */
    public function toApiArray()
    {
        return [
            'id'          => $this->id,
            'tipo_lista'  => $this->tipo_lista,
            'codigo'      => $this->codigo,
            'descripcion' => $this->descripcion,
        ];
    }
}
