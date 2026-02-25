<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class MacroFoto extends Model
{
    protected $table = 'macro_fotos';

    protected $fillable = [
        'macromedidor_id',
        'ruta_foto',
    ];

    public function macromedidor()
    {
        return $this->belongsTo(Macromedidor::class, 'macromedidor_id');
    }
}
