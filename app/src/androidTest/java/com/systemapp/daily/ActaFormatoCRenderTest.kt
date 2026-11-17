package com.systemapp.daily

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.systemapp.daily.data.acta.ActaFormatoC
import com.systemapp.daily.data.model.EstadoOrden
import com.systemapp.daily.data.model.HidraulicoEntity
import com.systemapp.daily.data.model.ListasRespaldo
import com.systemapp.daily.data.model.RevisionEntity
import com.systemapp.daily.data.model.TipoLista
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Renderiza el acta del Formato C con datos de ejemplo (58 mm, 80 mm y PDF) en
 * files/acta_test/ para revisar el diseño. No toca la BD.
 */
@RunWith(AndroidJUnit4::class)
class ActaFormatoCRenderTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(ctx.filesDir, "acta_test").apply { mkdirs() }

    private fun firmaFalsa(nombre: String): String {
        val bmp = Bitmap.createBitmap(600, 200, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 6f; style = Paint.Style.STROKE }
        c.drawPath(Path().apply { moveTo(40f, 150f); cubicTo(150f, 10f, 250f, 190f, 330f, 60f); cubicTo(400f, 10f, 480f, 170f, 560f, 90f) }, p)
        val f = File(dir, "$nombre.png")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f.absolutePath
    }

    private fun datos(): ActaFormatoC.Datos {
        val rev = RevisionEntity(
            idOrden = 925, codigoPredio = "143610", estadoOrden = EstadoOrden.EJECUTADO,
            numeroRevision = "925", periodoAnio = "2024", periodoMes = "3", tipoConsumo = "BAJO",
            nombreSuscriptor = "CARABALI ZAPATA CARLOS HECTOR", direccion = "QUINAMAYO PLAYA AMARILLA",
            refMedidor = "14044378", lecturaPeriodo = "3204", lecturaAnterior = "3203", consumoActual = "1", promedio = "34",
            tipoPredio = "H", usoPredio = "R", numPersonas = 4, tiempoHabitacion = "2 años",
            lecturaActual = "3210", fechaVisita = "2026-11-17 10:32:00", medidorMovimiento = "CON",
            causasConsumo = "[\"AUSENCIA_HABITANTES\",\"FUGA_IMPERCEPTIBLE\",\"OTRO\"]", causaOtroCual = "Tanque elevado rebosado",
            generalidades = "Se encontró el predio habitado. El usuario manifiesta que estuvo de viaje el mes anterior.",
            nombreAtiende = "María Carabalí", estadoAcometida = "BUENO", estadoSellos = "INTACTOS", numFamilias = 1,
            gpsLatitudPredio = 3.0123456, gpsLongitudPredio = -76.5123456,
            tipoDocumento = "CC", documento = "1061234567",
            firmaCliente = firmaFalsa("firma_cliente"), firmaFuncionario = firmaFalsa("firma_funcionario"),
            funcionarioNombre = "Juan Pérez", funcionarioCc = "10203040"
        )
        return ActaFormatoC.Datos(
            empresa = "ACUASUR", revision = rev,
            censo = listOf(HidraulicoEntity(idRevision = 925, tipoPunto = "SANITARIO", cantidad = 2, estado = "BUENO"),
                HidraulicoEntity(idRevision = 925, tipoPunto = "DUCHA", cantidad = 1, estado = "MALO")),
            tiposPredio = ListasRespaldo.get(TipoLista.TIPO_PREDIO),
            usosPredio = ListasRespaldo.get(TipoLista.USO_PREDIO),
            movimientos = ListasRespaldo.get(TipoLista.MOVIMIENTO_MEDIDOR),
            causas = ListasRespaldo.get(TipoLista.CAUSA_CONSUMO),
            descAcometida = "Bueno", descSellos = "Intactos",
            funcionarioNombre = "Juan Pérez", funcionarioCc = "10203040",
            firmaClientePath = rev.firmaCliente, firmaFuncionarioPath = rev.firmaFuncionario
        )
    }

    @Test
    fun renderizaAnchosYPdf() {
        val d = datos()
        for (papel in ActaFormatoC.Papel.values()) {
            val bmp = ActaFormatoC.renderizar(d, papel)
            assertTrue(bmp.width == papel.puntos && bmp.height > 500)
            FileOutputStream(File(dir, "acta_${papel.name}.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }
        val pdf = ActaFormatoC.generarPdf(ctx, d)
        assertTrue(pdf.length() > 1000)
        pdf.copyTo(File(dir, "acta.pdf"), overwrite = true)
    }
}
