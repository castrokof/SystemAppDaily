# Revisión de lo construido vs. formato C y CRITICA 02 — qué falta

Fecha: 2026-11-17. Fuentes revisadas: `CAMBIOS_REVISIONES_MACROS.md`, `COMPARATIVO_PRESENTACION.md`,
`docs/formato_c.jpeg`, `docs/CRITICA 02.XLS`, y el código (`OrdenRevision`, `RevisionApiController`, `RevisionController`, acta, seeder).

---

## 1. Qué es cada documento de Acuasur

### 1.1 `CRITICA 02.XLS` — "Primera crítica de consumos" (sale del aplicativo comercial)
- Archivo en formato **Excel antiguo (BIFF2)**, no `.xls` moderno. Lo pude leer con un parser propio, pero **Maatwebsite/PhpSpreadsheet no lo abre**: hay que guardarlo como `.xlsx`/`.csv` antes de cargarlo (o pedir al otro aplicativo que exporte `.xlsx`).
- Cabecera: empresa, fecha/hora de impresión, `PRIMERA CRITICA DE CONSUMOS CICLO 2 · AÑO 2026 · MES 11`. Pie: `TOTAL CUENTAS EN CRITICA: 16`.
- Encabezados de columna (fila 3): `Cod suscrip | Dirección | Cat | Estr | <6 meses de consumo: MAY…OCT> | Lec-act | Lec-ant | Consum | Promed | Cnl | Obs | T desviación | Dv max | Medidor`.
- Contenido (16 cuentas, ciclo 2): `Obs` = **alta / baja** (tipo de desviación); `Cnl` = un código que solo aparece en algunas filas (valor `16` en las de consumo 0 y "baja"; significado por confirmar con el comercial); `T desviación` = diferencia contra el límite (positivo en alta, negativo en baja); `Cat` = categoría (`ES`, `OF`, `TE`…); `Estr` = estrato.
- Observaciones de calidad: dirección `_` en varias cuentas, algunas celdas vacías (Cnl, Dv max), `Medidor` mezcla texto y número (`19138917`, `0`).

### 1.2 `formato_c.jpeg` — "Revisión de crítica de lectura No. 925" (formato físico)
Se imprime **antes** de la visita con datos precargados y el revisor llena a mano el resto. "Copia usuario".

| Bloque | Campos |
|---|---|
| Precargado | N° de revisión, fecha y hora de impresión, periodo (año, mes), **Alto consumo / Bajo consumo**, nombre suscriptor, código interno, N° medidor, dirección, lectura actual, lectura anterior, consumo, promedio |
| A llenar — predio | **Predio**: H / D / C · **Uso**: R / C / I / O / E · N° de habitantes · tiempo de habitación |
| A llenar — visita | Lectura al momento de la visita · fecha de visita · hora de visita |
| Informe de visita | Medidor **sin movimiento** / **con movimiento** |
| Causa del consumo (marcas) | Aumento de habitantes · En construcción · Daño · Ausencia de habitantes · Otro (cuál) · Fuga perceptible en baño · Fuga perceptible en llave terminal · Fuga imperceptible · Otro (cuál) |
| Cierre | Observación (2 líneas) · Firma usuario + C.C. · Funcionario que realizó la actividad |

Confirmado por el usuario: **Predio** H = Habitado, D = Desocupado, C = Cortado; **Uso** R = Residencial, C = Comercial, I = Industrial, O = Oficial, E = Especial. Por confirmar: por qué "Otro (cuál)" aparece dos veces.

---

## 2. Lo que ya cubre el sistema (resumen de los .md)
- Revisiones completas: críticas con filtros AJAX, modal con foto + gráfica 6 meses (con lectura y causado), asignación en un paso por periodo/ciclo/ruta/consecutivo, carga de predios por archivo con prevalidación, tablero, GPS, acta ticket 80 mm, reasignación.
- Clientes (listado + ficha), macromedidores y balance mensual de pérdidas, `ClienteSync` al cargar el Excel, menú con opción activa.
- API móvil: `ordenesRevision`, `revisionesMovilV2`, `listasParametros`, `ordenesMacro`, `macromedidoresMovil`.

---

## 3. Brechas contra el formato C (lo más importante)

### 3.1 Datos que NO existen hoy en `ordenes_revision`
| Campo del formato C | Estado actual | Propuesta |
|---|---|---|
| N° de revisión (925) | Solo `id` interno | Mostrar `id` (o consecutivo propio `numero_revision` por periodo si quieren numeración que reinicie) |
| Periodo año/mes | No se guarda (se saca de `lectura`) | Copiar `periodo_anio`, `periodo_mes` al crear |
| Alto / Bajo consumo | No se guarda (hay `motivo_revision` DESVIACION_ALTA/BAJA, que sí sirve) | Mostrarlo como "Alto/Bajo consumo" en formato y actas; añadir `OTRO` ⇒ vacío |
| Predio H/D/C | No existe | `tipo_predio` (lista `TIPO_PREDIO`) |
| Uso R/C/I/O/E | No existe en la revisión (`ordenescu.uso` existe) | `uso_predio` (lista `USO_PREDIO`), precargado desde `ordenescu.uso` y editable |
| N° habitantes | Existe como `num_personas` | Reutilizar (renombrar solo en etiquetas) |
| Tiempo de habitación | No existe | `tiempo_habitacion` (texto: "2 años", "3 meses") |
| Lectura al momento de la visita | Existe `nueva_lectura` (pero la API **no la devuelve**) | Reutilizar `nueva_lectura`, mostrarla como "Lectura visita" |
| Fecha y hora de visita | `fecha_cierre` (momento de sincronizar, no de la visita) | Añadir `fecha_visita` tomada en el celular al ejecutar |
| Medidor con / sin movimiento | No existe | `medidor_movimiento` (`CON`/`SIN`) |
| Causa del consumo (multi-marca) | No existe | Tabla `revision_causas` (`revision_id`, `codigo`, `detalle`) o JSON; lista `CAUSA_CONSUMO` |
| Otro (cuál) | `motivo_detalle` mezcla otra cosa | `causa_otro_cual` |
| Observación | `generalidades` | Reutilizar como "Observación" |
| Firma usuario + C.C. | `firma_cliente` + `documento` | Ya cubierto |
| Funcionario que realizó | Solo `usuario_id` (nombre) | Mostrar nombre + C.C. del funcionario; añadir `documento` en usuario o campo `funcionario_cc`; opcional firma del funcionario |

### 3.2 Campos que ya tenemos y el formato C no trae (decidir si se conservan)
Censo hidráulico, estado de acometida, estado de sellos, número de familias, tipo de documento, GPS del predio, fotos. Recomendación: **conservarlos como opcionales** (la app los muestra en un paso "Información adicional"), porque son evidencia útil, pero que el flujo principal sea el del formato C para no alargar la visita.

### 3.3 Brechas en el servidor
1. **`toApiArray()` no envía lo que el formato necesita**: hoy devuelve solo `codigo_predio` y los campos de ejecución. La app no recibe nombre, dirección, medidor, lecturas, consumo, promedio, crítica, periodo ni alto/bajo → no puede pintar el encabezado del formato C. Hay que añadirlos (ver `CAMBIOS_APP_ANDROID_REVISIONES.md`).
2. `nueva_lectura` se guarda pero no se devuelve en `ordenesRevision`.
3. **Impresión previa del formato C** (lote por revisor/ruta, con datos precargados y casillas en blanco, "Copia usuario" y "Copia empresa"): no existe. Hoy solo hay acta ticket 80 mm posterior a la visita.
4. **Acta posterior** debería reproducir el formato C llenado (mismas secciones y orden) para que lo firmado digitalmente se parezca a lo que ellos ya manejan.
5. Tablero/listado/ficha del cliente no muestran causa del consumo, medidor con/sin movimiento ni lectura de la visita.
6. `listasParametros` necesita las listas nuevas: `TIPO_PREDIO`, `USO_PREDIO`, `CAUSA_CONSUMO`, `MOVIMIENTO_MEDIDOR` (seeder).
7. Informe/consolidado de causas (cuántas revisiones por causa, por ciclo, por ruta) — es lo que justifica el formato.

---

## 4. Brechas contra CRITICA 02 (crítica "del otro aplicativo")

1. **No se puede cargar tal cual.** `leerSuscriptoresArchivo()`:
   - solo busca el encabezado en la **primera fila** del archivo; en CRITICA 02 la primera fila es `ACUASUR` y el encabezado `Cod suscrip` está en la fila 3 → cae a "primera columna" y mete en la lista textos como `ACUASUR`, `PRIMERA CRITICA…`, `TOTAL CUENTAS EN CRITICA:` (saldrían como "suscriptores sin lectura");
   - `Cod suscrip` no está entre las claves reconocidas (`suscriptor, codigo, codigo_predio, predio, cuenta, matricula`);
   - el `.XLS` es BIFF2 (ilegible para PhpSpreadsheet).
   **Arreglo propuesto:** buscar el encabezado en las primeras ~10 filas, aceptar `cod suscrip`/`cod. suscrip`, descartar filas no numéricas y la fila `TOTAL…`, y mostrar un mensaje claro si el formato no es legible ("guárdelo como .xlsx").
2. **Aprovechar más datos del archivo.** Además del suscriptor, el archivo trae `Obs` (alta/baja), `T desviación`, `Cnl`, `Cat` y `Estr`. Se podría guardar `alto/bajo` y desviación al crear la revisión (para que el formato C salga con "Alto/Bajo consumo" igual que el comercial) en vez de recalcularlo con nuestra regla (`<50 %` / `>200 %` del promedio), que **puede no coincidir con la del comercial**.
3. **Regla de crítica no documentada.** No se puede deducir de las 16 filas cuál es el límite alto/bajo ni qué significa `Dv max` y `Cnl 16`. Pedir al comercial la fórmula; si nuestras críticas (`Critica` en `ordenescu`) difieren de las suyas, el director verá dos listas distintas de "críticos".
4. **Comparador crítica comercial vs. crítica del sistema** (nuevo): cargar el archivo y mostrar cuáles cuentas están en una y no en la otra. Es útil para validar antes de reemplazar su aplicativo.
5. **Exportar nuestra crítica con el mismo formato de columnas** (`Cod suscrip, Dirección, Cat, Estr, 6 meses, Lec-act, Lec-ant, Consum, Promed, Cnl, Obs, T desviación, Medidor`) en Excel moderno: la tabla de `revisiones/criticas` hoy no muestra los 6 meses ni alto/bajo; solo el modal. Añadir columnas (o un botón "Exportar crítica").
6. `Cat` y `Estr`: el sistema no maneja estrato (decisión previa: sin facturación/estratos). Si Acuasur los quiere ver, sacarlos del archivo importado o de `ordenescu.uso/servicio`, solo informativos.
7. Ceros a la izquierda / suscriptores numéricos de Excel (pendiente ya anotado): `54568` vs `054568` pueden no cruzar.

---

## 5. Lo de censo y demás pendientes de los .md

- **Censo hidráulico**: la tabla existe, pero la ficha del cliente y el acta solo lo muestran por revisión; falta (a) vista de "último censo del cliente", (b) comparar censo vs. consumo (¿cuántos puntos hay vs. cuánto consume?), (c) decidir si el censo es obligatorio en el nuevo flujo de formato C (propuesta: opcional).
- Migración `2026_11_17_000009` (menús Cargar predios, Clientes, etc.) **sin ejecutar**; las dos filas insertadas a mano ya existen.
- **Balance diario** (mismo día de la lectura del macro): solo falta la consulta; los datos ya están.
- **Pruebas en navegador** pendientes: críticas, asignar, clientes (Chart.js/Leaflet por CDN), macromedidores, GPS revisiones, carga de Excel con cambio de medidor.
- Los cambios de medidor previos al 2026-11-17 no están en `cliente_series`.
- Se anotó una vista de inicio nueva (`landing`) sin probar en navegador.
- Archivos de copia sueltos sin trackear: `EntradaController copy.php`, `EntradaImport copy.php`, `EntradaImport copy 2.php` (riesgo de clases duplicadas/ruido; revisar y borrar antes de commitear).
- Casi todo el trabajo está **sin commit** (git: 27 archivos modificados + muchos nuevos). Conviene hacer commits por bloque (revisiones, clientes, macros, landing).
- Seguridad/robustez: `acta_pdf` y firmas se guardan en `public/uploads` sin validar tipo/tamaño del archivo (API); validar MIME y tamaño máximo.

## 6. `COMPARATIVO_PRESENTACION.md` quedó desactualizado
Está escrito como "sistema actual vs. demo" (fecha 2026-11-14) y afirma que el sistema actual tiene **0 modelos de cliente**, sin macromedidores ni revisiones. Eso ya no es cierto: acuasur ahora tiene clientes, macromedidores + balance, revisiones y críticas con AJAX. Hay que reescribir las secciones 2, 3.6, 3.7 y las tablas 4 y 9 (o rehacer el comparativo "hoy vs. demo" desde cero) antes de usarlo con el director. Además, ese brief dice que la revisión de campo es de **acta con censo y firma**; con el formato C el flujo cambia, así que también hay que ajustar esa diapositiva.

---

## 7. Orden sugerido de trabajo
1. **Backend revisiones (formato C)**: migración con campos nuevos (3.1), seeder de listas nuevas, `toApiArray` completo, `enviarRevisionV2` aceptando los campos nuevos. *(Bloquea a la app Android.)*
2. **Arreglo de lectura de archivo de crítica** (4.1) + guardar alto/bajo y desviación.
3. **Impresión de formato C por lote** y acta posterior con el mismo diseño.
4. **Exportar crítica** con columnas de Acuasur + comparador (4.4, 4.5).
5. Reportes de causas de consumo y censo (5).
6. Actualizar comparativo/presentación, commits y pruebas en navegador.
7. Mientras tanto, la app Android puede empezar con `CAMBIOS_APP_ANDROID_REVISIONES.md` en cuanto el paso 1 esté desplegado.
