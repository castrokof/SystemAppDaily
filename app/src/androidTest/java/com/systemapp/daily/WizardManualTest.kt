package com.systemapp.daily

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.RevisionDto
import com.systemapp.daily.ui.revision_v2.RevisionWizardActivity
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Prueba manual asistida: siembra una orden de ejemplo y abre el wizard por un rato
 * para recorrerlo en el emulador. Solo se ejecuta pidiéndola explícitamente
 * (-e class com.systemapp.daily.WizardManualTest).
 */
@RunWith(AndroidJUnit4::class)
class WizardManualTest {

    @Test
    fun abrirWizardConOrdenDeEjemplo() {
        // Solo con: adb shell am instrument -w -e manual true -e class com.systemapp.daily.WizardManualTest ...
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("manual") == "true")
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val session = SessionManager(ctx)
        session.userName = "Juan Pérez"
        session.userEmpresa = "ACUASUR"
        // firma del funcionario (como si la hubiera registrado en Menú > Firma)
        val firma = File(ctx.filesDir, "firmas").apply { mkdirs() }.let { File(it, "firma_revisor_test.png") }
        val bmp = Bitmap.createBitmap(500, 160, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply { drawColor(Color.WHITE); drawLine(30f, 120f, 470f, 40f, Paint().apply { strokeWidth = 6f }) }
        FileOutputStream(firma).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        session.firmaPath = firma.absolutePath

        val json = """
            [{"id_orden": 925, "codigo_predio": "143610", "estado_orden": "PENDIENTE",
              "numero_revision": 925, "periodo_anio": 2024, "periodo_mes": 3, "tipo_consumo": "BAJO",
              "nombre_suscriptor": "CARABALI ZAPATA CARLOS HECTOR", "direccion": "QUINAMAYO PLAYA AMARILLA",
              "ref_medidor": "14044378", "lectura_actual": 3204, "lectura_anterior": 3203,
              "consumo_actual": 1, "promedio": 34, "uso_predio": "R", "causas_consumo": [],
              "funcionario_nombre": "Juan Pérez", "funcionario_cc": "10203040"}]
        """.trimIndent()
        val dtos = com.google.gson.Gson().fromJson(json, Array<RevisionDto>::class.java).toList()
        runBlocking { AppDatabase.getDatabase(ctx).ordenRevisionDao().guardarDescarga(dtos) }

        val intent = Intent(ctx, RevisionWizardActivity::class.java).putExtra(Constants.EXTRA_ORDEN_ID, 925)
        ActivityScenario.launch<RevisionWizardActivity>(intent).use {
            Thread.sleep(MINUTOS * 60_000L)
        }
    }

    companion object {
        private const val MINUTOS = 6
    }
}
