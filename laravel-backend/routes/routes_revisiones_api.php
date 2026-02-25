<?php

/*
|--------------------------------------------------------------------------
| RUTAS API - Revisiones + Listas Parametros
|--------------------------------------------------------------------------
|
| Agregar en routes/api.php
| O en app/Http/routes.php con prefijo 'api/' (Laravel 5.2 o menor).
|
*/

// Descargar ordenes de revision del usuario
// GET  /api/ordenesRevision?api_token=xxx
Route::get('ordenesRevision', 'Api\RevisionApiController@ordenesRevision');

// Subir revision ejecutada (multipart con fotos, firma, censo)
// POST /api/revisionesMovilV2
Route::post('revisionesMovilV2', 'Api\RevisionApiController@enviarRevisionV2');

// Descargar listas de parametros (dropdowns de la app)
// GET  /api/listasParametros?api_token=xxx
Route::get('listasParametros', 'Api\RevisionApiController@listasParametros');


/*
|--------------------------------------------------------------------------
| NOTA PARA LARAVEL 5.2 o anterior (app/Http/routes.php)
|--------------------------------------------------------------------------
|
| Route::group(['prefix' => 'api'], function () {
|     Route::get('ordenesRevision', 'Api\RevisionApiController@ordenesRevision');
|     Route::post('revisionesMovilV2', 'Api\RevisionApiController@enviarRevisionV2');
|     Route::get('listasParametros', 'Api\RevisionApiController@listasParametros');
| });
|
*/
