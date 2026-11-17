package com.systemapp.daily.data.acta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.CausasJson
import com.systemapp.daily.data.model.HidraulicoEntity
import com.systemapp.daily.data.model.ListasRespaldo
import com.systemapp.daily.data.model.Opcion
import com.systemapp.daily.data.model.RevisionEntity
import com.systemapp.daily.data.model.TipoLista
import com.systemapp.daily.utils.SessionManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Acta de la "Revisión de crítica de lectura" (Formato C de Acuasur).
 *
 * Un único diseño dibujado con Canvas que se usa para:
 *  - imprimir en térmica de 58 mm (384 puntos) o 80 mm (576 puntos), y
 *  - generar el PDF (`acta_pdf`) que se sube al servidor, idéntico a lo impreso.
 */
object ActaFormatoC {

    enum class Papel(val etiqueta: String, val puntos: Int, val textoBase: Float) {
        MM58("58 mm", 384, 21f),
        MM80("80 mm", 576, 24f);

        companion object {
            fun from(mm: Int) = if (mm == 58) MM58 else MM80
        }
    }

    data class Datos(
        val empresa: String,
        val revision: RevisionEntity,
        val censo: List<HidraulicoEntity>,
        val tiposPredio: List<Opcion>,
        val usosPredio: List<Opcion>,
        val movimientos: List<Opcion>,
        val causas: List<Opcion>,
        val descAcometida: String?,
        val descSellos: String?,
        val funcionarioNombre: String,
        val funcionarioCc: String?,
        val firmaClientePath: String?,
        val firmaFuncionarioPath: String?,
        val fechaImpresion: Date = Date()
    )

    /** Arma los datos del acta desde la BD (listas del servidor o respaldo local). */
    suspend fun construirDatos(context: Context, revision: RevisionEntity): Datos {
        val db = AppDatabase.getDatabase(context)
        val session = SessionManager(context)
        val listaDao = db.listaDao()

        suspend fun lista(tipo: String): List<Opcion> {
            val remotas = listaDao.getByTipo(tipo).map { Opcion(it.codigo, it.descripcion) }
            return remotas.ifEmpty { ListasRespaldo.get(tipo) }
        }

        fun desc(opciones: List<Opcion>, codigo: String?) =
            codigo?.let { c -> opciones.firstOrNull { it.codigo == c }?.descripcion ?: c }

        return Datos(
            empresa = session.userEmpresa?.takeIf { it.isNotBlank() } ?: "ACUEDUCTO",
            revision = revision,
            censo = db.hidraulicoDao().getByRevision(revision.idOrden),
            tiposPredio = lista(TipoLista.TIPO_PREDIO),
            usosPredio = lista(TipoLista.USO_PREDIO),
            movimientos = lista(TipoLista.MOVIMIENTO_MEDIDOR),
            causas = lista(TipoLista.CAUSA_CONSUMO),
            descAcometida = desc(lista(TipoLista.ACOMETIDA), revision.estadoAcometida),
            descSellos = desc(lista(TipoLista.SELLOS), revision.estadoSellos),
            funcionarioNombre = revision.funcionarioNombre?.takeIf { it.isNotBlank() }
                ?: session.userName ?: session.userUsuario ?: "",
            funcionarioCc = revision.funcionarioCc,
            firmaClientePath = revision.firmaCliente,
            firmaFuncionarioPath = revision.firmaFuncionario ?: session.firmaPath
        )
    }

    /** Imagen del acta al ancho del papel (lista para enviar a la impresora). */
    fun renderizar(datos: Datos, papel: Papel, escala: Float = 1f): Bitmap {
        val ancho = (papel.puntos * escala).toInt()
        val firmas = cargarFirmas(datos)
        try {
            // 1ª pasada: medir alto. 2ª: dibujar.
            val alto = Lienzo(ancho, papel, escala, null).also { dibujar(it, datos, firmas) }.y
            val bitmap = Bitmap.createBitmap(ancho, (alto + 1).toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            dibujar(Lienzo(ancho, papel, escala, canvas), datos, firmas)
            return bitmap
        } finally {
            firmas.cliente?.recycle()
            firmas.funcionario?.recycle()
        }
    }

    /**
     * PDF de una sola página con el mismo diseño de 80 mm (226.8 pt de ancho).
     * Se renderiza al doble de resolución para que el texto quede nítido.
     */
    fun generarPdf(context: Context, datos: Datos): File {
        val bitmap = renderizar(datos, Papel.MM80, escala = 2f)
        val anchoPt = 227
        val altoPt = (bitmap.height * anchoPt.toFloat() / bitmap.width).toInt().coerceAtLeast(1)
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(anchoPt, altoPt, 1).create())
            val factor = anchoPt.toFloat() / bitmap.width
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawBitmap(bitmap, Matrix().apply { setScale(factor, factor) }, Paint(Paint.FILTER_BITMAP_FLAG))
            doc.finishPage(page)

            val dir = File(context.filesDir, "actas").apply { mkdirs() }
            val file = File(dir, "acta_revision_${datos.revision.idOrden}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            return file
        } finally {
            doc.close()
            bitmap.recycle()
        }
    }

    // ------------------------------------------------------------------
    // Diseño
    // ------------------------------------------------------------------

    private class Firmas(val cliente: Bitmap?, val funcionario: Bitmap?)

    private fun cargarFirmas(d: Datos) = Firmas(decodificar(d.firmaClientePath), decodificar(d.firmaFuncionarioPath))

    private fun decodificar(path: String?): Bitmap? {
        if (path.isNullOrBlank() || !File(path).exists()) return null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sample = 1
            while (opts.outWidth / sample > 1200) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: Throwable) { null }
    }

    private fun dibujar(l: Lienzo, d: Datos, firmas: Firmas) {
        val r = d.revision
        val fmt = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale("es", "CO"))

        // ---- Encabezado ----
        l.texto(d.empresa.uppercase(), l.titulo, Paint.Align.CENTER)
        l.texto("Copia usuario", l.pequeno, Paint.Align.RIGHT)
        l.espacio(4)
        l.texto("REVISIÓN DE CRÍTICA DE LECTURA", l.negrita, Paint.Align.CENTER)
        l.texto("No. ${r.numeroVisible}", l.subtitulo, Paint.Align.CENTER)
        l.texto("Impresión: ${fmt.format(d.fechaImpresion)}", l.pequeno, Paint.Align.CENTER)
        l.separador()

        // ---- Datos precargados ----
        val periodo = listOfNotNull(r.periodoAnio, r.periodoMes?.padStart(2, '0')).joinToString(" / ")
        l.par("Periodo:", periodo.ifBlank { "-" }, "Código:", r.codigoPredio)
        r.etiquetaConsumo?.let { l.etiquetaInvertida(it) }
        l.campo("Suscriptor:", r.nombreSuscriptor)
        l.campo("Medidor:", r.refMedidor)
        l.campo("Dirección:", r.direccion)
        l.par("Lect. actual:", r.lecturaPeriodo, "Lect. anterior:", r.lecturaAnterior)
        l.par("Consumo:", r.consumoActual, "Promedio:", r.promedio)
        l.separador()

        // ---- Predio ----
        l.opciones("Predio:", d.tiposPredio.map { it.codigo to (it.codigo == r.tipoPredio) })
        l.opciones("Uso:", d.usosPredio.map { it.codigo to (it.codigo == r.usoPredio) })
        l.par("N° habitantes:", r.numPersonas?.toString(), "Tiempo hab.:", r.tiempoHabitacion)
        l.campo("Lectura visita:", r.lecturaActual)
        val (fechaV, horaV) = separarFechaHora(r.fechaVisita)
        l.par("Fecha visita:", fechaV, "Hora:", horaV)
        l.separador()

        // ---- Informe de visita ----
        l.texto("INFORME DE VISITA", l.negrita)
        l.opcionesLista(d.movimientos.map { "Medidor ${it.descripcion.lowercase()}" to (it.codigo == r.medidorMovimiento) })
        l.espacio(4)
        l.texto("Causa del consumo:", l.negrita)
        val marcadas = CausasJson.parse(r.causasConsumo).toSet()
        l.opcionesLista(d.causas.map { op ->
            val etiqueta = if (op.codigo == ListasRespaldo.CAUSA_OTRO && !r.causaOtroCual.isNullOrBlank())
                "${op.descripcion}: ${r.causaOtroCual}" else op.descripcion
            etiqueta to (op.codigo in marcadas)
        })
        l.espacio(4)
        l.texto("Observación:", l.negrita)
        l.texto(r.generalidades?.takeIf { it.isNotBlank() } ?: "Sin observación", l.normal)
        l.separador()

        // ---- Información adicional (solo si hay algo) ----
        val adicional = buildList {
            r.nombreAtiende?.takeIf { it.isNotBlank() }?.let { add("Atiende:" to it) }
            d.descAcometida?.let { add("Acometida:" to it) }
            d.descSellos?.let { add("Sellos:" to it) }
            r.numFamilias?.let { add("N° familias:" to it.toString()) }
            if (r.gpsLatitudPredio != null && r.gpsLongitudPredio != null)
                add("GPS:" to String.format(Locale.US, "%.6f, %.6f", r.gpsLatitudPredio, r.gpsLongitudPredio))
            if (r.fotos.isNotEmpty()) add("Fotos:" to r.fotos.size.toString())
        }
        if (adicional.isNotEmpty() || d.censo.isNotEmpty()) {
            l.texto("INFORMACIÓN ADICIONAL", l.negrita)
            adicional.forEach { (k, v) -> l.campo(k, v) }
            if (d.censo.isNotEmpty()) {
                l.texto("Censo hidráulico:", l.negrita)
                d.censo.forEach { l.texto("• ${it.tipoPunto} x${it.cantidad} (${it.estado})", l.normal) }
            }
            l.separador()
        }

        // ---- Firmas ----
        val docCliente = if (!r.documento.isNullOrBlank()) "${r.tipoDocumento ?: "C.C."} ${r.documento}" else "C.C. ____________"
        l.firma(firmas.cliente, "Firma usuario", docCliente)
        l.espacio(10)
        l.firma(firmas.funcionario, "Funcionario que realizó la actividad",
            buildString {
                append(d.funcionarioNombre)
                d.funcionarioCc?.takeIf { it.isNotBlank() }?.let { append("\nC.C. $it") }
            })
        l.espacio(16)
    }

    private fun separarFechaHora(valor: String?): Pair<String?, String?> {
        if (valor.isNullOrBlank()) return null to null
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(valor) ?: return valor to null
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(d) to SimpleDateFormat("h:mm a", Locale("es", "CO")).format(d)
        } catch (_: Exception) { valor to null }
    }

    /**
     * Cursor de dibujo. Con [canvas] = null solo mide (para saber el alto total).
     */
    private class Lienzo(val ancho: Int, papel: Papel, val escala: Float, val canvas: Canvas?) {
        var y = 0f
        private val margen = 6f * escala
        private val ancho80 = papel == Papel.MM80
        private val base = papel.textoBase * escala

        private fun paint(size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = size
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

        val normal = paint(base)
        val negrita = paint(base, true)
        val pequeno = paint(base * 0.82f)
        val subtitulo = paint(base * 1.25f, true)
        val titulo = paint(base * 1.4f, true)
        private val linea = Paint().apply { color = Color.BLACK; strokeWidth = 2f * escala; style = Paint.Style.STROKE }
        private val relleno = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        private val util get() = ancho - 2 * margen

        fun espacio(px: Int) { y += px * escala }

        private fun altoLinea(p: Paint) = p.fontSpacing

        /** Texto con ajuste de línea (respeta saltos \n). */
        fun texto(t: String, p: Paint, align: Paint.Align = Paint.Align.LEFT) {
            for (parrafo in t.split("\n")) {
                for (lineaTxt in partir(parrafo, p, util)) {
                    y += altoLinea(p)
                    val x = when (align) {
                        Paint.Align.LEFT -> margen
                        Paint.Align.CENTER -> ancho / 2f
                        Paint.Align.RIGHT -> ancho - margen
                    }
                    canvas?.drawText(lineaTxt, x, y - p.descent(), Paint(p).apply { textAlign = align })
                }
            }
        }

        /** "Etiqueta: valor" con la etiqueta en negrita y el valor ajustado a la derecha de ella. */
        fun campo(etiqueta: String, valor: String?, x0: Float = margen, anchoCol: Float = util) {
            val v = valor?.takeIf { it.isNotBlank() } ?: "-"
            val wEt = negrita.measureText("$etiqueta ")
            val disponible = anchoCol - wEt
            val lineas = if (disponible > anchoCol * 0.35f) partir(v, normal, disponible) else listOf()
            if (lineas.isEmpty()) {
                y += altoLinea(negrita); canvas?.drawText(etiqueta, x0, y - negrita.descent(), negrita)
                partir(v, normal, anchoCol).forEach { y += altoLinea(normal); canvas?.drawText(it, x0, y - normal.descent(), normal) }
                return
            }
            lineas.forEachIndexed { i, txt ->
                y += altoLinea(normal)
                if (i == 0) canvas?.drawText(etiqueta, x0, y - normal.descent(), negrita)
                canvas?.drawText(txt, x0 + wEt, y - normal.descent(), normal)
            }
        }

        /** Dos campos en la misma fila en 80 mm; uno debajo del otro en 58 mm. */
        fun par(e1: String, v1: String?, e2: String, v2: String?) {
            if (!ancho80) { campo(e1, v1); campo(e2, v2); return }
            val col = util / 2f
            val y0 = y
            campo(e1, v1, margen, col - 4 * escala)
            val y1 = y
            y = y0
            campo(e2, v2, margen + col, col)
            y = maxOf(y, y1)
        }

        /** Fila tipo "Predio:  ☒ H  ☐ D  ☐ C" que se parte si no cabe. */
        fun opciones(titulo: String, items: List<Pair<String, Boolean>>) {
            val h = altoLinea(normal)
            val caja = normal.textSize * 0.8f
            var x = margen + negrita.measureText("$titulo ")
            y += h
            canvas?.drawText(titulo, margen, y - normal.descent(), negrita)
            for ((txt, marcado) in items) {
                val w = caja + 6 * escala + normal.measureText(txt) + 14 * escala
                if (x + w > ancho - margen) { x = margen + 12 * escala; y += h }
                dibujarCaja(x, y - normal.descent() - caja, caja, marcado)
                canvas?.drawText(txt, x + caja + 6 * escala, y - normal.descent(), normal)
                x += w
            }
        }

        /** Lista vertical de casillas (causas del consumo, movimiento del medidor). */
        fun opcionesLista(items: List<Pair<String, Boolean>>) {
            val caja = normal.textSize * 0.8f
            val sangria = margen + caja + 8 * escala
            for ((txt, marcado) in items) {
                val lineas = partir(txt, if (marcado) negrita else normal, ancho - margen - sangria)
                lineas.forEachIndexed { i, s ->
                    y += altoLinea(normal)
                    if (i == 0) dibujarCaja(margen, y - normal.descent() - caja, caja, marcado)
                    canvas?.drawText(s, sangria, y - normal.descent(), if (marcado) negrita else normal)
                }
            }
        }

        private fun dibujarCaja(x: Float, top: Float, lado: Float, marcado: Boolean) {
            val c = canvas ?: return
            val rect = RectF(x, top, x + lado, top + lado)
            c.drawRect(rect, linea)
            if (marcado) {
                c.drawLine(rect.left + 3 * escala, rect.top + 3 * escala, rect.right - 3 * escala, rect.bottom - 3 * escala, linea)
                c.drawLine(rect.right - 3 * escala, rect.top + 3 * escala, rect.left + 3 * escala, rect.bottom - 3 * escala, linea)
            }
        }

        /** Texto blanco sobre fondo negro (ALTO / BAJO CONSUMO). */
        fun etiquetaInvertida(t: String) {
            val p = Paint(negrita).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
            val h = altoLinea(p) + 8 * escala
            canvas?.drawRect(margen, y + 2 * escala, ancho - margen, y + h, relleno)
            canvas?.drawText(t, ancho / 2f, y + h - 4 * escala - p.descent(), p)
            y += h + 2 * escala
        }

        fun separador() {
            y += 6 * escala
            canvas?.drawLine(margen, y, ancho - margen, y, linea)
            y += 4 * escala
        }

        /** Imagen de firma (si existe) + raya + leyenda. */
        fun firma(bmp: Bitmap?, leyenda: String, detalle: String) {
            val maxAlto = 110f * escala
            if (bmp != null && bmp.width > 0) {
                val anchoMax = util * 0.8f
                val factor = minOf(anchoMax / bmp.width, maxAlto / bmp.height)
                val w = bmp.width * factor
                val h = bmp.height * factor
                val left = (ancho - w) / 2f
                canvas?.drawBitmap(bmp, null, RectF(left, y, left + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
                y += h
            } else {
                y += maxAlto * 0.6f
            }
            canvas?.drawLine(margen + util * 0.1f, y, ancho - margen - util * 0.1f, y, linea)
            texto(leyenda, negrita, Paint.Align.CENTER)
            texto(detalle, normal, Paint.Align.CENTER)
        }

        private fun partir(texto: String, p: Paint, ancho: Float): List<String> {
            if (texto.isEmpty()) return listOf("")
            val res = mutableListOf<String>()
            var resto = texto.trim()
            while (resto.isNotEmpty()) {
                var n = p.breakText(resto, true, ancho, null)
                if (n <= 0) n = 1
                if (n < resto.length) {
                    val corte = resto.lastIndexOf(' ', n)
                    if (corte > 0) n = corte
                }
                res += resto.substring(0, n).trimEnd()
                resto = resto.substring(n).trimStart()
            }
            return res
        }
    }
}
