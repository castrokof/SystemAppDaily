# Endpoints que debes crear en tu API Laravel

La app Android usa tu endpoint existente `loginMovil1` y necesita 3 endpoints nuevos.

## Endpoint Existente (ya lo tienes)

### `GET /api/loginMovil1`
Login del usuario. La app lo llama igual que ahora.

**Parámetros (query):**
- `usuario` (string) - nombre de usuario
- `password` (string) - contraseña

**Respuesta esperada:**
```json
{
  "success": true,
  "message": "Login exitoso",
  "token": "tu_token_aqui",
  "user": {
    "id": 1,
    "nombre": "Juan Pérez",
    "usuario": "pruebas",
    "email": "juan@email.com",
    "rol": "lector"
  }
}
```

> **Nota:** Si tu endpoint actual retorna campos diferentes, ajusta el archivo
> `app/src/main/java/com/systemapp/daily/data/model/LoginResponse.kt`
> para que coincida con tu respuesta real.

---

## Endpoints Nuevos (debes crearlos)

### 1. `GET /api/macrosMovil`
Lista los macromedidores asignados al usuario autenticado.

**Headers:**
- `Authorization: Bearer {token}`

**Respuesta:**
```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "codigo": "MAC-001",
      "nombre": "Macro Principal Zona Norte",
      "direccion": "Calle 10 #5-20",
      "descripcion": "Macromedidor de entrada zona norte",
      "estado": "activo",
      "ultima_lectura": "2026-02-09 14:30:00",
      "lecturas_hoy": 1,
      "lectura_autorizada": false
    },
    {
      "id": 2,
      "codigo": "MAC-002",
      "nombre": "Macro Zona Sur",
      "direccion": "Carrera 15 #20-45",
      "descripcion": null,
      "estado": "activo",
      "ultima_lectura": null,
      "lecturas_hoy": 0,
      "lectura_autorizada": false
    }
  ]
}
```

**Campos importantes:**
- `lecturas_hoy`: cuántas lecturas ha hecho el usuario hoy para ese macro
- `lectura_autorizada`: `true` si desde la web autorizaste que haga más de 2 lecturas hoy

**Ejemplo en Laravel (routes/api.php):**
```php
Route::get('macrosMovil', function (Request $request) {
    $user = auth()->user(); // o como manejes auth

    $macros = Macro::whereHas('usuarios', function ($q) use ($user) {
        $q->where('user_id', $user->id);
    })->get()->map(function ($macro) use ($user) {
        $hoy = Carbon::today()->toDateString();
        $lecturasHoy = Lectura::where('macro_id', $macro->id)
            ->where('usuario_id', $user->id)
            ->whereDate('fecha', $hoy)
            ->count();

        $autorizacion = AutorizacionLectura::where('macro_id', $macro->id)
            ->where('usuario_id', $user->id)
            ->whereDate('fecha', $hoy)
            ->where('activa', true)
            ->exists();

        return [
            'id' => $macro->id,
            'codigo' => $macro->codigo,
            'nombre' => $macro->nombre,
            'direccion' => $macro->direccion,
            'descripcion' => $macro->descripcion,
            'estado' => $macro->estado,
            'ultima_lectura' => $macro->lecturas()->latest()->first()?->fecha,
            'lecturas_hoy' => $lecturasHoy,
            'lectura_autorizada' => $autorizacion,
        ];
    });

    return response()->json(['success' => true, 'data' => $macros]);
})->middleware('auth:api');
```

---

### 2. `GET /api/lecturasMovil/check`
Verifica si el usuario puede tomar lectura de un macro hoy.

**Headers:**
- `Authorization: Bearer {token}`

**Parámetros (query):**
- `macro_id` (int) - ID del macromedidor

**Respuesta:**
```json
{
  "success": true,
  "puede_leer": true,
  "lecturas_hoy": 1,
  "max_lecturas": 2,
  "autorizado_extra": false,
  "message": "Puede tomar 1 lectura más hoy"
}
```

**Cuando el límite se alcanza:**
```json
{
  "success": true,
  "puede_leer": false,
  "lecturas_hoy": 2,
  "max_lecturas": 2,
  "autorizado_extra": false,
  "message": "Ya alcanzó el límite de 2 lecturas hoy. Solicite autorización."
}
```

**Cuando está autorizado para más:**
```json
{
  "success": true,
  "puede_leer": true,
  "lecturas_hoy": 3,
  "max_lecturas": 2,
  "autorizado_extra": true,
  "message": "Lectura autorizada por administrador"
}
```

---

### 3. `POST /api/lecturasMovil` (Multipart)
Envía una lectura con fotos.

**Headers:**
- `Authorization: Bearer {token}`
- `Content-Type: multipart/form-data`

**Body (form-data):**
- `macro_id` (int) - ID del macromedidor
- `valor_lectura` (string) - valor leído del medidor
- `observacion` (string, opcional) - observación del lector
- `fotos[0]` (file) - primera foto (obligatoria)
- `fotos[1]` (file) - segunda foto (obligatoria)
- `fotos[2]` ... (file) - fotos adicionales (opcionales, máx 5)

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Lectura registrada correctamente",
  "data": {
    "id": 123,
    "lecturas_restantes": 1,
    "puede_leer": true
  }
}
```

**Respuesta cuando no puede leer:**
```json
{
  "success": false,
  "message": "Límite de lecturas alcanzado. No autorizado para lecturas adicionales.",
  "data": null
}
```

**Ejemplo en Laravel:**
```php
Route::post('lecturasMovil', function (Request $request) {
    $request->validate([
        'macro_id' => 'required|integer|exists:macros,id',
        'valor_lectura' => 'required|string',
        'observacion' => 'nullable|string',
        'fotos' => 'required|array|min:2|max:5',
        'fotos.*' => 'image|mimes:jpeg,png,jpg|max:10240',
    ]);

    $user = auth()->user();
    $hoy = Carbon::today()->toDateString();

    // Verificar límite
    $lecturasHoy = Lectura::where('macro_id', $request->macro_id)
        ->where('usuario_id', $user->id)
        ->whereDate('fecha', $hoy)
        ->count();

    $autorizado = AutorizacionLectura::where('macro_id', $request->macro_id)
        ->where('usuario_id', $user->id)
        ->whereDate('fecha', $hoy)
        ->where('activa', true)
        ->exists();

    if ($lecturasHoy >= 2 && !$autorizado) {
        return response()->json([
            'success' => false,
            'message' => 'Límite de lecturas alcanzado.',
            'data' => null
        ], 403);
    }

    // Guardar lectura
    $lectura = Lectura::create([
        'macro_id' => $request->macro_id,
        'usuario_id' => $user->id,
        'valor_lectura' => $request->valor_lectura,
        'observacion' => $request->observacion,
        'fecha' => now(),
    ]);

    // Guardar fotos
    foreach ($request->file('fotos') as $foto) {
        $path = $foto->store('lecturas/' . $lectura->id, 'public');
        LecturaFoto::create([
            'lectura_id' => $lectura->id,
            'ruta' => $path,
        ]);
    }

    $lecturasRestantes = $autorizado ? 999 : (2 - ($lecturasHoy + 1));

    return response()->json([
        'success' => true,
        'message' => 'Lectura registrada correctamente',
        'data' => [
            'id' => $lectura->id,
            'lecturas_restantes' => max(0, $lecturasRestantes),
            'puede_leer' => $lecturasRestantes > 0 || $autorizado,
        ]
    ]);
})->middleware('auth:api');
```

---

## Tablas sugeridas para la BD

### `macros`
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | bigint PK | |
| codigo | varchar | Código único del macro |
| nombre | varchar | Nombre descriptivo |
| direccion | varchar nullable | Ubicación |
| descripcion | text nullable | |
| estado | varchar | activo/inactivo |
| created_at | timestamp | |
| updated_at | timestamp | |

### `macro_usuario` (pivote)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| macro_id | bigint FK | |
| user_id | bigint FK | |

### `lecturas`
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | bigint PK | |
| macro_id | bigint FK | |
| usuario_id | bigint FK | |
| valor_lectura | varchar | Valor leído |
| observacion | text nullable | |
| fecha | datetime | |
| created_at | timestamp | |
| updated_at | timestamp | |

### `lectura_fotos`
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | bigint PK | |
| lectura_id | bigint FK | |
| ruta | varchar | Path del archivo |
| created_at | timestamp | |

### `autorizacion_lecturas`
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | bigint PK | |
| macro_id | bigint FK | |
| usuario_id | bigint FK | |
| fecha | date | Día para el que se autoriza |
| activa | boolean | Si la autorización está vigente |
| autorizado_por | bigint FK | Admin que autorizó |
| created_at | timestamp | |
