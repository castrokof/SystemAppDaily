<?php

/*
|--------------------------------------------------------------------------
| RUTAS WEB - Revisiones
|--------------------------------------------------------------------------
|
| Agregar en routes/web.php (o app/Http/routes.php en Laravel 5.2)
| dentro del grupo middleware 'auth'.
|
*/

Route::group(['middleware' => 'auth'], function () {

    // Vista de criticas (supervisor ve lecturas con Estado=4)
    Route::get('revisiones/criticas', 'RevisionController@criticas')
        ->name('revisiones.criticas');

    // AJAX: marcar lecturas para revision (Coordenada = 'generar')
    Route::post('revisiones/adicionar-critica', 'RevisionController@adicionarcritica')
        ->name('revisiones.adicionar-critica');

    // AJAX: desmarcar lecturas (Coordenada = NULL)
    Route::post('revisiones/eliminar-critica', 'RevisionController@eliminarcritica')
        ->name('revisiones.eliminar-critica');

    // Generar ordenes de revision desde las marcadas
    Route::post('revisiones/generar', 'RevisionController@generar')
        ->name('revisiones.generar');

    // Reasignar revisor
    Route::post('revisiones/{id}/reasignar', 'RevisionController@reasignar')
        ->name('revisiones.reasignar');

    // Listado y detalle de revisiones
    Route::get('revisiones', 'RevisionController@index')
        ->name('revisiones.index');
    Route::get('revisiones/{id}', 'RevisionController@show')
        ->name('revisiones.show');
    Route::delete('revisiones/{id}', 'RevisionController@destroy')
        ->name('revisiones.destroy');

    // Gestion de listas parametros
    Route::get('listas-parametros', 'RevisionController@listas')
        ->name('listas.index');
});
