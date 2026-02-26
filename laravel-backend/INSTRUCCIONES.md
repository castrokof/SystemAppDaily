# Backend Laravel 5 - Macromedidores + Revisiones

## Estructura de archivos - Donde copiar cada archivo

```
tu-proyecto-laravel/
├── database/
│   ├── migrations/
│   │   ├── 2024_01_01_000001_create_macromedidores_table.php
│   │   └── 2024_01_01_000002_create_revisiones_table.php
│   └── seeds/
│       └── ListasParametrosSeeder.php
│
├── app/
│   ├── Macromedidor.php
│   ├── MacroFoto.php
│   ├── OrdenRevision.php
│   ├── CensoHidraulico.php
│   ├── RevisionFoto.php
│   ├── ListaParametro.php
│   └── Http/Controllers/
│       ├── MacromedidorController.php
│       ├── RevisionController.php
│       └── Api/
│           ├── MacromedidorApiController.php
│           └── RevisionApiController.php
│
├── resources/views/
│   ├── macromedidores/
│   │   ├── index.blade.php
│   │   ├── create.blade.php
│   │   ├── show.blade.php
│   │   └── edit.blade.php
│   └── revisiones/
│       ├── criticas.blade.php    (supervisor selecciona lecturas)
│       ├── index.blade.php       (listado ordenes generadas)
│       ├── show.blade.php        (detalle con resultado wizard)
│       └── listas.blade.php      (ver listas parametros)
│
└── routes/ (o app/Http/routes.php en Laravel 5.2)
```

## Pasos de instalacion

### 1. Copiar las migraciones y ejecutar
```bash
cp migrations/*.php database/migrations/
php artisan migrate
```

### 2. Copiar el seeder y ejecutar
```bash
cp seeders/ListasParametrosSeeder.php database/seeds/
php artisan db:seed --class=ListasParametrosSeeder
```

### 3. Copiar los Models
```bash
cp models/Macromedidor.php app/
cp models/MacroFoto.php app/
cp models/OrdenRevision.php app/
cp models/CensoHidraulico.php app/
cp models/RevisionFoto.php app/
cp models/ListaParametro.php app/
```

### 4. Copiar los Controllers
```bash
cp controllers/MacromedidorController.php app/Http/Controllers/
cp controllers/RevisionController.php app/Http/Controllers/
mkdir -p app/Http/Controllers/Api
cp controllers/MacromedidorApiController.php app/Http/Controllers/Api/
cp controllers/RevisionApiController.php app/Http/Controllers/Api/
```

### 5. Agregar las rutas

**Si tu Laravel tiene `routes/web.php` y `routes/api.php`:**
- Copiar contenido de `routes/routes_web.php` en `routes/web.php`
- Copiar contenido de `routes/routes_revisiones_web.php` en `routes/web.php`
- Copiar contenido de `routes/routes_api.php` en `routes/api.php`
- Copiar contenido de `routes/routes_revisiones_api.php` en `routes/api.php`

**Si tu Laravel usa `app/Http/routes.php` (Laravel 5.2 o menor):**
```php
// ======= MACROMEDIDORES =======
Route::resource('macromedidores', 'MacromedidorController');
Route::post('macromedidores/{id}/resetear', 'MacromedidorController@resetear');

// ======= REVISIONES =======
Route::get('revisiones/criticas', 'RevisionController@criticas')->name('revisiones.criticas');
Route::post('revisiones/adicionar-critica', 'RevisionController@adicionarcritica')->name('revisiones.adicionar-critica');
Route::post('revisiones/eliminar-critica', 'RevisionController@eliminarcritica')->name('revisiones.eliminar-critica');
Route::post('revisiones/generar', 'RevisionController@generar')->name('revisiones.generar');
Route::post('revisiones/{id}/reasignar', 'RevisionController@reasignar')->name('revisiones.reasignar');
Route::get('revisiones', 'RevisionController@index')->name('revisiones.index');
Route::get('revisiones/{id}', 'RevisionController@show')->name('revisiones.show');
Route::delete('revisiones/{id}', 'RevisionController@destroy')->name('revisiones.destroy');
Route::get('listas-parametros', 'RevisionController@listas')->name('listas.index');

// ======= API =======
Route::group(['prefix' => 'api'], function () {
    // Macromedidores
    Route::get('ordenesMacro', 'Api\MacromedidorApiController@ordenesMacro');
    Route::post('macromedidoresMovil', 'Api\MacromedidorApiController@enviarMacro');
    // Revisiones
    Route::get('ordenesRevision', 'Api\RevisionApiController@ordenesRevision');
    Route::post('revisionesMovilV2', 'Api\RevisionApiController@enviarRevisionV2');
    Route::get('listasParametros', 'Api\RevisionApiController@listasParametros');
});
```

### 6. Crear carpetas de uploads
```bash
mkdir -p public/uploads/macros
mkdir -p public/uploads/revisiones/fotos
mkdir -p public/uploads/revisiones/firmas
mkdir -p public/uploads/revisiones/actas
chmod -R 775 public/uploads
```

### 7. Copiar las vistas
```bash
mkdir -p resources/views/macromedidores
mkdir -p resources/views/revisiones
cp views/macromedidores/*.blade.php resources/views/macromedidores/
cp views/revisiones/*.blade.php resources/views/revisiones/
```

---

## Flujo de trabajo

```
LECTURAS (ordenescu)
    |
    | Critica != "CONSUMO NORMAL"
    v
SUPERVISOR (/revisiones/criticas)
    |
    | Selecciona lecturas + asigna revisor
    | POST /revisiones/generar
    v
ORDENES DE REVISION (ordenes_revision)
    |
    | Estado: PENDIENTE
    | Se descargan a la app: GET /api/ordenesRevision
    v
APP MOVIL (wizard 5 pasos)
    |
    | Revisor ejecuta en campo
    | POST /api/revisionesMovilV2
    v
REVISION EJECUTADA
    |
    | Estado: EJECUTADO + sincronizado
    | Ver en: /revisiones/{id}
    v
DETALLE con fotos, firma, censo, GPS
```

---

## Endpoints API

### Macromedidores

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| GET | `/api/ordenesMacro?api_token=xxx` | Descarga ordenes macro |
| POST | `/api/macromedidoresMovil` | Sube lectura + fotos |

### Revisiones

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| GET | `/api/ordenesRevision?api_token=xxx` | Descarga ordenes revision |
| POST | `/api/revisionesMovilV2` | Sube revision ejecutada (wizard completo) |
| GET | `/api/listasParametros?api_token=xxx` | Descarga listas para dropdowns |

### POST revisionesMovilV2 (campos)
```
api_token, id_orden, codigo_predio,
estado_acometida, estado_sellos,
nombre_atiende, tipo_documento, documento,
num_familias, num_personas,
motivo_revision, motivo_detalle, generalidades,
censo_hidraulico_json (JSON array),
gps_latitud, gps_longitud,
fotos[] (archivos), firma_cliente (archivo), acta_pdf (archivo)
```

---

## URLs Web

### Macromedidores
| URL | Descripcion |
|-----|-------------|
| `/macromedidores` | Listado con filtros |
| `/macromedidores/create` | Formulario crear macro |
| `/macromedidores/{id}` | Ver detalle + fotos + lectura |
| `/macromedidores/{id}/edit` | Editar datos basicos |

### Revisiones
| URL | Descripcion |
|-----|-------------|
| `/revisiones/criticas` | Supervisor: ver lecturas criticas y generar ordenes |
| `/revisiones` | Listado ordenes de revision |
| `/revisiones/{id}` | Detalle: 5 pasos del wizard, fotos, firma, censo, GPS |
| `/listas-parametros` | Ver/verificar listas de parametros |

---

## Listas de Parametros (Seeder)

| Tipo | Valores |
|------|---------|
| MOTIVOS | DESVIACION_BAJA, DESVIACION_ALTA, FRAUDE, MEDIDOR_DANADO, SIN_LECTURA, OTRO |
| ACOMETIDA | BUENA, REGULAR, MALA, DIRECTA, NO_VISIBLE |
| SELLOS | INTACTOS, ROTOS, SIN_SELLOS, MANIPULADOS |
| TIPO_DOCUMENTO | CC, CE, TI, NIT, PP |
| TIPO_PUNTO | GRIFO, SANITARIO, DUCHA, LAVAMANOS, LAVAPLATOS, LAVADERO, TANQUE, CALENTADOR, OTRO |
| SURTE | ACUEDUCTO, POZO, TANQUE_ELEVADO, PILA_PUBLICA, OTRO |

## Nota sobre las vistas

Las vistas usan `@extends('layouts.app')`. Ajusta segun tu layout principal.
Usan Bootstrap 3 con FontAwesome (panel, label, table, form-group, etc.)
que es el estandar de Laravel 5.
