<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class CensoHidraulico extends Model
{
    protected $table = 'censo_hidraulico';

    protected $fillable = [
        'revision_id',
        'tipo_punto',
        'cantidad',
        'estado',
    ];

    protected $casts = [
        'cantidad' => 'integer',
    ];

    public function revision()
    {
        return $this->belongsTo(OrdenRevision::class, 'revision_id');
    }
}
