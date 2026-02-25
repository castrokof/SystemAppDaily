<?php

/*
|--------------------------------------------------------------------------
| RUTAS API - Macromedidores
|--------------------------------------------------------------------------
|
| Agregar estas rutas en tu archivo routes/api.php
| (o en routes/web.php con prefijo api/ si usas Laravel 5.2 o menor).
|
| NOTA: Si tu Laravel 5 NO tiene carpeta routes/ y usa app/Http/routes.php,
| agrega estas rutas ahi con el prefijo 'api/'.
|
| URL Base: https://manteliviano.com/AquaProgrammerData/api/
|
*/

// ============================================================
// API MOVIL - Endpoints para la app Android
// ============================================================

// Descargar ordenes macro del usuario
// GET  /api/ordenesMacro?api_token=xxx
Route::get('ordenesMacro', 'Api\MacromedidorApiController@ordenesMacro');

// Subir lectura con fotos (multipart)
// POST /api/macromedidoresMovil
Route::post('macromedidoresMovil', 'Api\MacromedidorApiController@enviarMacro');


/*
|--------------------------------------------------------------------------
| NOTA IMPORTANTE PARA LARAVEL 5.2 o anterior
|--------------------------------------------------------------------------
|
| Si tu proyecto NO tiene routes/api.php, agrega estas rutas en
| app/Http/routes.php asi:
|
| Route::group(['prefix' => 'api'], function () {
|     Route::get('ordenesMacro', 'Api\MacromedidorApiController@ordenesMacro');
|     Route::post('macromedidoresMovil', 'Api\MacromedidorApiController@enviarMacro');
| });
|
*/
