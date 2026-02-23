package com.systemapp.daily.ui.revision

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.systemapp.daily.data.model.ChecklistItem
import com.systemapp.daily.data.model.EstadoCheck
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Genera el PDF del acta de revisión de servicio de acueducto.
 * Incluye: información del medidor, checklist, observaciones, GPS, firmas.
 */
class ActaPdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595  // A4
        private const val PAGE_HEIGHT = 842 // A4
        private const val MARGIN = 40f
        private const val LINE_HEIGHT = 16f
    }

    data class ActaData(
        val empresa: String,
        val refMedidor: String,
        val suscriptor: String?,
        val direccion: String?,
        val medidorNombre: String,
        val checklistItems: List<ChecklistItem>,
        val observacion: String?,
        val latitud: Double?,
        val longitud: Double?,
        val usuario: String,
        val firmaUsuario: Bitmap?,
        val firmaCliente: Bitmap?
    )

    fun generarActaPdf(data: ActaData): File {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = MARGIN

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 11f
            isAntiAlias = true
        }

        val smallPaint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 9f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        val greenPaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.parseColor("#C62828")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val grayPaint = Paint().apply {
            color = Color.parseColor("#757575")
            textSize = 11f
            isAntiAlias = true
        }

        // --- ENCABEZADO ---
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        canvas.drawText("ACTA DE REVISION DE SERVICIO", MARGIN, yPos + 18f, titlePaint)
        yPos += 24f
        canvas.drawText("DE ACUEDUCTO", MARGIN, yPos + 18f, titlePaint)
        yPos += 30f

        // Línea azul decorativa
        val blueLine = Paint().apply {
            color = Color.parseColor("#1565C0")
            strokeWidth = 3f
        }
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, blueLine)
        yPos += 15f

        // Empresa y fecha
        canvas.drawText("Empresa: ${data.empresa ?: "N/A"}", MARGIN, yPos + 11f, boldPaint)
        canvas.drawText("Fecha: $fecha", PAGE_WIDTH - MARGIN - textPaint.measureText("Fecha: $fecha"), yPos + 11f, textPaint)
        yPos += LINE_HEIGHT + 4f

        canvas.drawText("Inspector: ${data.usuario}", MARGIN, yPos + 11f, textPaint)
        yPos += LINE_HEIGHT + 8f

        // --- DATOS DEL SUSCRIPTOR ---
        val boxPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        val boxBorder = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 72f, boxPaint)
        canvas.drawRect(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos + 72f, boxBorder)
        yPos += 4f

        canvas.drawText("DATOS DEL SUSCRIPTOR", MARGIN + 8f, yPos + 13f, headerPaint)
        yPos += LINE_HEIGHT + 4f
        canvas.drawText("Ref. Medidor: ${data.refMedidor}", MARGIN + 8f, yPos + 11f, boldPaint)
        canvas.drawText("Suscriptor: ${data.suscriptor ?: "N/A"}", PAGE_WIDTH / 2f, yPos + 11f, textPaint)
        yPos += LINE_HEIGHT + 2f
        canvas.drawText("Nombre: ${data.medidorNombre}", MARGIN + 8f, yPos + 11f, textPaint)
        yPos += LINE_HEIGHT + 2f
        canvas.drawText("Dirección: ${data.direccion ?: "N/A"}", MARGIN + 8f, yPos + 11f, textPaint)
        yPos += LINE_HEIGHT + 12f

        // --- GPS ---
        if (data.latitud != null && data.longitud != null) {
            canvas.drawText(
                "Ubicación GPS: ${String.format("%.6f", data.latitud)}, ${String.format("%.6f", data.longitud)}",
                MARGIN, yPos + 11f, smallPaint
            )
            yPos += LINE_HEIGHT + 4f
        }

        // --- CHECKLIST ---
        canvas.drawText("RESULTADO DE LA REVISION", MARGIN, yPos + 13f, headerPaint)
        yPos += LINE_HEIGHT + 8f

        // Tabla header
        val col1 = MARGIN
        val col2 = MARGIN + 120f
        val col3 = PAGE_WIDTH - MARGIN - 80f

        canvas.drawText("Categoría", col1, yPos + 11f, boldPaint)
        canvas.drawText("Descripción", col2, yPos + 11f, boldPaint)
        canvas.drawText("Estado", col3, yPos + 11f, boldPaint)
        yPos += 4f
        canvas.drawLine(MARGIN, yPos + LINE_HEIGHT - 4f, PAGE_WIDTH - MARGIN, yPos + LINE_HEIGHT - 4f, linePaint)
        yPos += LINE_HEIGHT

        var currentCategoria = ""
        for (item in data.checklistItems) {
            // Verificar si necesitamos nueva página
            if (yPos > PAGE_HEIGHT - 200f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = MARGIN
            }

            val showCategoria = if (item.categoria != currentCategoria) {
                currentCategoria = item.categoria
                item.categoria
            } else {
                ""
            }

            canvas.drawText(showCategoria, col1, yPos + 11f, smallPaint)

            // Truncar descripción si es muy larga
            val desc = if (item.descripcion.length > 40) item.descripcion.substring(0, 37) + "..." else item.descripcion
            canvas.drawText(desc, col2, yPos + 11f, textPaint)

            val estadoPaint = when (item.estado) {
                EstadoCheck.BUENO -> greenPaint
                EstadoCheck.MALO -> redPaint
                EstadoCheck.NO_APLICA -> grayPaint
                EstadoCheck.NO_REVISADO -> grayPaint
            }
            canvas.drawText(item.estado.label, col3, yPos + 11f, estadoPaint)

            yPos += LINE_HEIGHT + 2f
            canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)
            yPos += 4f
        }

        yPos += 8f

        // --- RESUMEN ---
        val buenos = data.checklistItems.count { it.estado == EstadoCheck.BUENO }
        val malos = data.checklistItems.count { it.estado == EstadoCheck.MALO }
        val na = data.checklistItems.count { it.estado == EstadoCheck.NO_APLICA }
        val sinRevisar = data.checklistItems.count { it.estado == EstadoCheck.NO_REVISADO }

        canvas.drawText("Resumen:", MARGIN, yPos + 11f, boldPaint)
        yPos += LINE_HEIGHT
        canvas.drawText("Buenos: $buenos  |  Malos: $malos  |  N/A: $na  |  Sin revisar: $sinRevisar", MARGIN, yPos + 11f, textPaint)
        yPos += LINE_HEIGHT + 8f

        // --- OBSERVACIONES ---
        if (!data.observacion.isNullOrBlank()) {
            if (yPos > PAGE_HEIGHT - 200f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = MARGIN
            }

            canvas.drawText("OBSERVACIONES", MARGIN, yPos + 13f, headerPaint)
            yPos += LINE_HEIGHT + 4f

            // Dividir observación en líneas
            val maxCharsPerLine = 75
            val obsLines = data.observacion.chunked(maxCharsPerLine)
            for (line in obsLines) {
                canvas.drawText(line, MARGIN, yPos + 11f, textPaint)
                yPos += LINE_HEIGHT
            }
            yPos += 8f
        }

        // --- FIRMAS ---
        // Verificar si hay espacio para las firmas, si no, nueva página
        if (yPos > PAGE_HEIGHT - 220f) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = MARGIN
        }

        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, blueLine)
        yPos += 12f

        canvas.drawText("FIRMAS", MARGIN, yPos + 13f, headerPaint)
        yPos += LINE_HEIGHT + 12f

        val firmaWidth = (PAGE_WIDTH - MARGIN * 3) / 2f
        val firmaHeight = 100f

        // Firma del inspector (usuario)
        val firmaUsuarioRect = RectF(MARGIN, yPos, MARGIN + firmaWidth, yPos + firmaHeight)
        val firmaClienteRect = RectF(MARGIN * 2 + firmaWidth, yPos, PAGE_WIDTH - MARGIN, yPos + firmaHeight)

        // Recuadro firmas
        val firmaBorder = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(firmaUsuarioRect, firmaBorder)
        canvas.drawRect(firmaClienteRect, firmaBorder)

        // Dibujar firmas
        if (data.firmaUsuario != null) {
            val scaled = Bitmap.createScaledBitmap(data.firmaUsuario, firmaWidth.toInt(), firmaHeight.toInt(), true)
            canvas.drawBitmap(scaled, firmaUsuarioRect.left, firmaUsuarioRect.top, null)
            scaled.recycle()
        }

        if (data.firmaCliente != null) {
            val scaled = Bitmap.createScaledBitmap(data.firmaCliente, firmaWidth.toInt(), firmaHeight.toInt(), true)
            canvas.drawBitmap(scaled, firmaClienteRect.left, firmaClienteRect.top, null)
            scaled.recycle()
        }

        yPos += firmaHeight + 4f

        // Líneas de firma
        canvas.drawLine(MARGIN + 10f, yPos, MARGIN + firmaWidth - 10f, yPos, linePaint)
        canvas.drawLine(MARGIN * 2 + firmaWidth + 10f, yPos, PAGE_WIDTH - MARGIN - 10f, yPos, linePaint)
        yPos += LINE_HEIGHT

        canvas.drawText("Firma del Inspector", MARGIN + 10f, yPos + 11f, boldPaint)
        canvas.drawText("Firma del Cliente", MARGIN * 2 + firmaWidth + 10f, yPos + 11f, boldPaint)
        yPos += LINE_HEIGHT
        canvas.drawText(data.usuario, MARGIN + 10f, yPos + 9f, smallPaint)
        canvas.drawText(data.suscriptor ?: data.medidorNombre, MARGIN * 2 + firmaWidth + 10f, yPos + 9f, smallPaint)

        yPos += LINE_HEIGHT + 16f

        // Pie de página
        canvas.drawText("Documento generado desde SystemApp Lecturas - $fecha", MARGIN, yPos + 9f, smallPaint)
        canvas.drawText("Copia para el cliente", PAGE_WIDTH - MARGIN - smallPaint.measureText("Copia para el cliente"), yPos + 9f, smallPaint)

        document.finishPage(page)

        // Guardar PDF
        val fileName = "acta_revision_${data.refMedidor}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }
}
