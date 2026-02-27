<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class RevisionFoto extends Model
{
    protected $table = 'revision_fotos';

    protected $fillable = [
        'revision_id',
        'ruta_foto',
    ];

    public function revision()
    {
        return $this->belongsTo(OrdenRevision::class, 'revision_id');
    }
}
