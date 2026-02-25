# Backend Laravel 5 - Macromedidores

## Estructura de archivos - Donde copiar cada archivo

```
tu-proyecto-laravel/
├── database/migrations/
│   └── 2024_01_01_000001_create_macromedidores_table.php
│
├── app/
│   ├── Macromedidor.php              (Model)
│   ├── MacroFoto.php                 (Model)
│   └── Http/Controllers/
│       ├── MacromedidorController.php     (Web CRUD)
│       └── Api/
│           └── MacromedidorApiController.php  (API Movil)
│
├── resources/views/macromedidores/
│   ├── index.blade.php
│   ├── create.blade.php
│   ├── show.blade.php
│   └── edit.blade.php
│
└── routes/ (o app/Http/routes.php en Laravel 5.2)
```

## Pasos de instalacion

### 1. Copiar la migracion
```
cp migrations/... database/migrations/
```
Ejecutar:
```bash
php artisan migrate
```

### 2. Copiar los Models
```
cp models/Macromedidor.php app/Macromedidor.php
cp models/MacroFoto.php app/MacroFoto.php
```

### 3. Copiar los Controllers
```
cp controllers/MacromedidorController.php app/Http/Controllers/
mkdir -p app/Http/Controllers/Api
cp controllers/MacromedidorApiController.php app/Http/Controllers/Api/
```

### 4. Agregar las rutas

**Si tu Laravel tiene `routes/web.php`:**
Copiar el contenido de `routes/routes_web.php` en `routes/web.php`
Copiar el contenido de `routes/routes_api.php` en `routes/api.php`

**Si tu Laravel usa `app/Http/routes.php` (Laravel 5.2 o menor):**
```php
// Agregar en app/Http/routes.php

// Web
Route::resource('macromedidores', 'MacromedidorController');
Route::post('macromedidores/{id}/resetear', 'MacromedidorController@resetear');

// API
Route::group(['prefix' => 'api'], function () {
    Route::get('ordenesMacro', 'Api\MacromedidorApiController@ordenesMacro');
    Route::post('macromedidoresMovil', 'Api\MacromedidorApiController@enviarMacro');
});
```

### 5. Crear carpeta de uploads
```bash
mkdir -p public/uploads/macros
chmod 775 public/uploads/macros
```

### 6. Copiar las vistas
```
mkdir -p resources/views/macromedidores
cp views/macromedidores/*.blade.php resources/views/macromedidores/
```

## Endpoints API

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| GET | `/api/ordenesMacro?api_token=xxx` | Descarga ordenes del usuario |
| POST | `/api/macromedidoresMovil` | Sube lectura con fotos (multipart) |

### GET ordenesMacro
```
GET /api/ordenesMacro?api_token=abc123
```
Response:
```json
[
  {
    "id_orden": 1,
    "codigo_macro": "MAC-001",
    "ubicacion": "Calle 5 #12-30",
    "lectura_anterior": 12500,
    "estado": "PENDIENTE",
    "lectura_actual": null,
    "observacion": null,
    "ruta_fotos": null,
    "gps_latitud_lectura": null,
    "gps_longitud_lectura": null,
    "fecha_lectura": null,
    "sincronizado": false
  }
]
```

### POST macromedidoresMovil
```
POST /api/macromedidoresMovil
Content-Type: multipart/form-data

api_token: abc123
id_orden: 1
lectura_actual: 13200
observacion: Sin novedad
gps_latitud: 7.123456
gps_longitud: -73.123456
fotos[0]: (archivo imagen)
fotos[1]: (archivo imagen)
```
Response:
```json
{
  "success": true,
  "message": "Lectura recibida correctamente",
  "id": 1
}
```

## URLs Web

| URL | Descripcion |
|-----|-------------|
| `/macromedidores` | Listado con filtros |
| `/macromedidores/create` | Formulario crear macro |
| `/macromedidores/{id}` | Ver detalle + fotos + lectura |
| `/macromedidores/{id}/edit` | Editar datos basicos |

## Nota sobre las vistas

Las vistas usan `@extends('layouts.app')`. Ajusta segun tu layout principal.
Usan Bootstrap 3 con FontAwesome (panel, label, table, form-group, etc.)
que es el estandar de Laravel 5.
