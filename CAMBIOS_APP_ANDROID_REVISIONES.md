# Brief para Claude (app Android) — Revisiones según "Formato C" de Acuasur

> **Para Claude que trabaja en la app móvil.** Este documento describe los cambios que necesita el módulo de **Revisiones** para que replique el formato físico de Acuasur "Revisión de crítica de lectura" (Formato C). El servidor (Laravel) se actualizará en paralelo; los campos nuevos están marcados **[NUEVO]**. Lo marcado **[CONFIRMAR]** depende de una respuesta del cliente.
>
> La app es **Android nativo en Kotlin** (según el usuario; por los nombres `RevisionEntity`, `ListaEntity`, `@ColumnInfo` usa **Room**, y se asume **Retrofit**). El contrato de API y los campos son independientes de la tecnología.

---

## 1. Objetivo
Hoy el revisor llena un wizard (acometida, sellos, quién atiende, familias, censo hidráulico, motivo, fotos, firma). Acuasur ya trabaja con un formato en papel (Formato C). El wizard debe **seguir el orden y los campos del Formato C**, mostrando los datos precargados arriba y pidiendo solo lo que el papel pide a mano. Lo que ya existe (censo, sellos, acometida, fotos, GPS) pasa a un paso **opcional** "Información adicional".

## 2. Formato C — pantallas / campos

### Encabezado (solo lectura, vienen precargados del servidor)
`N° de revisión` · `Periodo (año / mes)` · `Alto consumo` o `Bajo consumo` · `Nombre suscriptor` · `Código interno` · `N° medidor` · `Dirección` · `Lectura actual` · `Lectura anterior` · `Consumo` · `Promedio`.
Mostrar todo en una tarjeta fija arriba del formulario.

### Paso 1 — Predio  *(obligatorio)*
- **Predio**: selección única (lista `TIPO_PREDIO`): `H` = Habitado, `D` = Desocupado, `C` = Cortado.
- **Uso del predio** (lista `USO_PREDIO`): `R` = Residencial, `C` = Comercial, `I` = Industrial, `O` = Oficial, `E` = Especial (confirmado). Preseleccionar con `uso_predio` si viene en la orden.
- **N° de habitantes**: entero → enviar en `num_personas`.
- **Tiempo de habitación**: texto corto, ej. "2 años" → `tiempo_habitacion`.

### Paso 2 — Visita  *(obligatorio)*
- **Lectura al momento de la visita**: entero → `nueva_lectura`.
- **Fecha y hora de visita**: se toman automáticamente del teléfono al abrir el paso (editable). Enviar ISO `yyyy-MM-dd HH:mm:ss` en `fecha_visita`.
- **Medidor**: `SIN movimiento` / `CON movimiento` (lista `MOVIMIENTO_MEDIDOR`, valores `SIN` / `CON`) → `medidor_movimiento`.

### Paso 3 — Causa del consumo  *(obligatorio al menos una; multi-selección con casillas)*
Lista `CAUSA_CONSUMO` (valores sugeridos):
| codigo | descripción |
|---|---|
| `AUMENTO_HABITANTES` | Aumento de habitantes |
| `EN_CONSTRUCCION` | En construcción |
| `DANO` | Daño |
| `AUSENCIA_HABITANTES` | Ausencia de habitantes |
| `FUGA_PERCEPTIBLE_BANO` | Fuga perceptible en baño |
| `FUGA_PERCEPTIBLE_LLAVE_TERMINAL` | Fuga perceptible en llave terminal |
| `FUGA_IMPERCEPTIBLE` | Fuga imperceptible |
| `OTRO` | Otro (cuál) → habilita campo de texto `causa_otro_cual` |

El formato en papel tiene dos "Otro (cuál)"; en la app basta uno (**[CONFIRMAR]** si necesitan dos).
**No hardcodear**: leer la lista desde `GET /api/listasParametros` (`tipo_lista = CAUSA_CONSUMO`) con respaldo local si no hay red.

### Paso 4 — Cierre  *(obligatorio)*
- **Observación**: texto multilínea → se sigue enviando en `generalidades` (es el mismo campo del servidor).
- **Firma del usuario** (canvas) + **C.C.** del usuario (`tipo_documento` + `documento`) → `firma_cliente`, `documento`.
- **Funcionario que realizó la actividad**: nombre y C.C. del usuario logueado (se muestran, no se escriben; vienen del login — ver §4.3). Opcional: firma del funcionario → `firma_funcionario` **[NUEVO, opcional]**.
- Botón "Guardar y sincronizar".

### Paso opcional — Información adicional (lo que ya existía)
Estado de acometida, estado de sellos, número de familias, quién atiende (`nombre_atiende`), censo hidráulico, fotos (ya soportadas) y GPS del predio. No bloquear el cierre si están vacíos.

### Validaciones de la app
- Obligatorios: predio, uso, habitantes, lectura de visita, movimiento del medidor, ≥1 causa (y `causa_otro_cual` si marcó `OTRO`), firma usuario.
- Lectura de visita: entero ≥ 0. Si es menor que la lectura anterior mostrar advertencia (no bloquear).
- Una revisión ya `EJECUTADO` y sincronizada no se edita; la pendiente de sincronizar sí.

---

## 3. Cambios en el contrato del API

## 3.0 Rutas del API para la app (todas bajo `/api`, ya existen salvo lo marcado)

Base: `https://<dominio>/acuasur/api/` (ajustar al `BASE_URL` que ya usa la app). Todas, excepto el login, llevan `api_token` (el mismo del login, como query `?api_token=` o `Authorization: Bearer`) y pasan por el middleware `auth.api.token`.

| Método | Ruta | Uso | Estado |
|---|---|---|---|
| POST | `loginMovil1` | Login; devuelve el token (y datos del usuario) | existe |
| GET | `ordenesRevision` | Descarga las revisiones asignadas al usuario | existe — **respuesta ampliada** |
| POST | `revisionesMovilV2` | Envía la revisión ejecutada (multipart) | existe — **campos nuevos** |
| GET | `listasParametros` | Listas de selectores | existe — **listas nuevas** |
| GET | `ordenesMacro` | Órdenes de macromedidor | existe |
| POST | `macromedidoresMovil` | Envía lectura de macromedidor | existe |
| POST | `medidoresout`, `medidores`, `marcas`, `medidorejecutado` | Lecturas normales (sin cambios) | existe |

Retrofit (Kotlin) para el módulo de revisiones:

```kotlin
interface RevisionApi {
    @GET("ordenesRevision")
    suspend fun ordenesRevision(@Query("api_token") token: String): List<RevisionDto>

    @GET("listasParametros")
    suspend fun listasParametros(@Query("api_token") token: String): List<ListaDto>

    @Multipart
    @POST("revisionesMovilV2")
    suspend fun enviarRevision(
        @PartMap campos: Map<String, @JvmSuppressWildcards RequestBody>,   // api_token, id_orden, tipo_predio, uso_predio, ...
        @Part fotos: List<MultipartBody.Part>,                              // name = "fotos[]"
        @Part firmaCliente: MultipartBody.Part?,                            // name = "firma_cliente"
        @Part firmaFuncionario: MultipartBody.Part?,                        // name = "firma_funcionario"
        @Part actaPdf: MultipartBody.Part?                                  // name = "acta_pdf"
    ): EnvioRevisionResponse
}
```
Los DTO deben declarar todos los campos [NUEVO] como `nullable` con valor por defecto (`= null`) para tolerar servidor viejo.


Autenticación sin cambios: `api_token` (Bearer / query) del login existente.

### 3.1 `GET /api/ordenesRevision`  — respuesta ampliada
Cada elemento conserva sus campos actuales (`id_orden`, `codigo_predio`, `estado_orden`, `estado_acometida`, …, `ruta_fotos`, `sincronizado`) y **agrega**:

```json
{
  "numero_revision": 925,                    // [NUEVO] puede ser el mismo id_orden
  "periodo_anio": 2024,                      // [NUEVO]
  "periodo_mes": 3,                          // [NUEVO]
  "tipo_consumo": "BAJO",                    // [NUEVO] "ALTO" | "BAJO" | null
  "nombre_suscriptor": "CARABALI ZAPATA CARLOS HECTOR", // [NUEVO]
  "direccion": "QUINAMAYO PLAYA AMARILLA",   // [NUEVO]
  "telefono": null,                          // [NUEVO]
  "ref_medidor": "14044378",                 // [NUEVO]
  "lectura_actual": 3204,                    // [NUEVO]
  "lectura_anterior": 3203,                  // [NUEVO]
  "consumo_actual": 1,                       // [NUEVO]
  "promedio": 34,                            // [NUEVO]
  "critica_original": "…",                   // [NUEVO]
  "uso_predio": "R",                         // [NUEVO] precarga, puede ser null
  "tipo_predio": null,                       // [NUEVO]
  "tiempo_habitacion": null,                 // [NUEVO]
  "nueva_lectura": null,                     // [NUEVO] (ya existía en BD, no se enviaba)
  "fecha_visita": null,                      // [NUEVO]
  "medidor_movimiento": null,                // [NUEVO] "SIN" | "CON"
  "causas_consumo": [],                      // [NUEVO] ["AUMENTO_HABITANTES", …]
  "causa_otro_cual": null,                   // [NUEVO]
  "funcionario_nombre": "…",                 // [NUEVO] usuario asignado
  "funcionario_cc": "…"                      // [NUEVO]
}
```
La app debe **tolerar campos faltantes o nulos** (servidor y app se despliegan en momentos distintos).

### 3.2 `POST /api/revisionesMovilV2` (multipart/form-data) — campos
Sin cambios: `id_orden`, `codigo_predio`, `estado_acometida`, `estado_sellos`, `nombre_atiende`, `tipo_documento`, `documento`, `num_familias`, `num_personas`, `motivo_revision`, `motivo_detalle`, `generalidades`, `censo_hidraulico_json`, `gps_latitud`, `gps_longitud`, `fotos[]`, `firma_cliente`, `acta_pdf`, `nueva_lectura`.

**Agregar:**
| campo | tipo | nota |
|---|---|---|
| `tipo_predio` | string | `H` / `D` / `C` |
| `uso_predio` | string | `R` / `C` / `I` / `O` / `E` |
| `tiempo_habitacion` | string | libre |
| `fecha_visita` | string | `yyyy-MM-dd HH:mm:ss` (hora del teléfono) |
| `medidor_movimiento` | string | `SIN` / `CON` |
| `causas_consumo_json` | string | JSON array de códigos, ej. `["DANO","FUGA_IMPERCEPTIBLE"]` |
| `causa_otro_cual` | string | requerido si incluye `OTRO` |
| `firma_funcionario` | file PNG | opcional |

`num_personas` = N° de habitantes del formato; `generalidades` = Observación; `nueva_lectura` = lectura al momento de la visita.
El servidor sigue respondiendo `{ "success": true, "id": … }`. Reenvíos del mismo `id_orden` siguen siendo idempotentes (reemplaza datos y censo).

### 3.3 `GET /api/listasParametros`
Se añaden estos `tipo_lista` (mismo formato que los actuales: `tipo_lista`, `codigo`, `descripcion`): `TIPO_PREDIO`, `USO_PREDIO`, `CAUSA_CONSUMO`, `MOVIMIENTO_MEDIDOR`. Las listas existentes (`MOTIVOS`, `ACOMETIDA`, `SELLOS`, `TIPO_DOCUMENTO`, `TIPO_PUNTO`, `SURTE`) no cambian.

---

## 4. Implementación en la app

### 4.1 Base de datos local (Room)
- Subir la **versión de la BD** y escribir migración (añadir columnas, no recrear tablas; no perder revisiones pendientes de sincronizar).
- `RevisionEntity`: agregar las columnas de §3.1/§3.2 (todas `nullable`). `causas_consumo` se guarda como JSON string o tabla relacionada con `TypeConverter`.
- `ListaEntity`: sin cambios de esquema (solo nuevos `tipo_lista`).

### 4.2 UI
- Reordenar el wizard según §2 (encabezado fijo + pasos 1-4 + paso opcional).
- Usar las listas descargadas para los selectores; chips/segmented buttons para H/D/C, R/C/I/O/E, SIN/CON; casillas para causas.
- Indicador "Alto consumo / Bajo consumo" con color (rojo / azul) en el encabezado.
- En el listado de órdenes mostrar: N° revisión, nombre, dirección, tipo de consumo y estado.

### 4.3 Funcionario
Mostrar `funcionario_nombre` y `funcionario_cc` (de la orden). Si el login ya devuelve la cédula del usuario, usarla; si no, pedir al servidor que la incluya (`funcionario_cc`).

### 4.4 Sincronización
- Mismo flujo actual (cola local → `revisionesMovilV2`), incluyendo reintentos.
- Enviar `fecha_visita` con la hora **del momento de la visita**, no de la sincronización.
- Si falla la red, la revisión queda local como pendiente de sincronizar; el cierre no se pierde.

### 4.5 Compatibilidad
- Versión nueva de la app contra servidor viejo: ignorar errores por campos desconocidos y no depender de los campos [NUEVO] para abrir la orden.
- Versión vieja de la app contra servidor nuevo: sigue funcionando (el servidor acepta los campos nuevos como opcionales).

---

## 5. Criterios de aceptación
1. Al abrir una orden se ve el encabezado del Formato C completo con datos del servidor.
2. El revisor puede llenar predio, uso, habitantes, tiempo de habitación, lectura/fecha/hora de visita, movimiento del medidor, causas (multi), observación y firma, en ese orden.
3. No puede cerrar sin los obligatorios; `OTRO` exige el texto "cuál".
4. Los datos llegan al servidor y se ven en el acta web de la revisión.
5. Sin internet: se guarda y sincroniza después sin duplicar (idempotente por `id_orden`).
6. El paso opcional (censo, acometida, sellos, fotos, GPS) sigue funcionando como antes.
7. Actualizar la app con revisiones locales pendientes no las pierde (migración Room).

## 6. Pendientes de confirmar con Acuasur antes de cerrar la app
- ¿Un solo "Otro (cuál)" o dos?
- ¿El censo hidráulico sigue siendo necesario o queda solo el Formato C?
- ¿Firma del funcionario en la app o basta nombre + C.C.?
- ¿La numeración "No. 925" debe reiniciar por periodo o usar el `id` global?
