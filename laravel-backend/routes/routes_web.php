<?php

/*
|--------------------------------------------------------------------------
| RUTAS WEB - Macromedidores
|--------------------------------------------------------------------------
|
| Agregar estas rutas en tu archivo routes/web.php
| dentro del grupo de middleware 'auth' (o como lo tengas).
|
*/

// ============================================================
// MACROMEDIDORES - CRUD Web (panel administrativo)
// ============================================================

Route::group(['middleware' => 'auth'], function () {

    Route::resource('macromedidores', 'MacromedidorController');

    // Ruta adicional para resetear una orden ejecutada a pendiente
    Route::post('macromedidores/{id}/resetear', 'MacromedidorController@resetear')
        ->name('macromedidores.resetear');
});
