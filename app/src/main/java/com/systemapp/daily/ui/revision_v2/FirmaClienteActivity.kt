package com.systemapp.daily.ui.revision_v2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.systemapp.daily.databinding.ActivityFirmaClienteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Firma del usuario a pantalla completa y en horizontal, para firmar cómodo.
 * Guarda el PNG en la ruta recibida y devuelve RESULT_OK.
 */
class FirmaClienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirmaClienteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirmaClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val destino = intent.getStringExtra(EXTRA_DESTINO)
        if (destino.isNullOrBlank()) { finish(); return }
        intent.getStringExtra(EXTRA_NOMBRE)?.takeIf { it.isNotBlank() }?.let { binding.tvNombre.text = it }

        binding.btnLimpiar.setOnClickListener { binding.signaturePad.clear() }
        binding.btnCancelar.setOnClickListener { setResult(Activity.RESULT_CANCELED); finish() }
        binding.btnAceptar.setOnClickListener { guardar(File(destino)) }
    }

    private fun guardar(destino: File) {
        if (binding.signaturePad.isEmpty()) {
            Toast.makeText(this, "Pida al usuario que firme en el recuadro", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnAceptar.isEnabled = false
        binding.progress.visibility = View.VISIBLE
        val bmp = binding.signaturePad.getSignatureBitmapRecortada()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    destino.parentFile?.mkdirs()
                    // Se escribe a un temporal y luego se renombra: nunca queda un PNG a medias
                    val tmp = File(destino.parentFile, destino.name + ".tmp")
                    FileOutputStream(tmp).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    tmp.renameTo(destino) || run { tmp.copyTo(destino, overwrite = true); tmp.delete() }
                } catch (_: Exception) { false } finally { bmp.recycle() }
            }
            if (ok) { setResult(Activity.RESULT_OK); finish() }
            else {
                binding.btnAceptar.isEnabled = true
                binding.progress.visibility = View.GONE
                Toast.makeText(this@FirmaClienteActivity, "No se pudo guardar la firma, intente de nuevo", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Entrada: ruta destino del PNG + nombre a mostrar. Salida: true si firmó. */
    class Contrato : ActivityResultContract<Pair<String, String?>, Boolean>() {
        override fun createIntent(context: Context, input: Pair<String, String?>) =
            Intent(context, FirmaClienteActivity::class.java)
                .putExtra(EXTRA_DESTINO, input.first)
                .putExtra(EXTRA_NOMBRE, input.second)

        override fun parseResult(resultCode: Int, intent: Intent?) = resultCode == Activity.RESULT_OK
    }

    companion object {
        private const val EXTRA_DESTINO = "destino"
        private const val EXTRA_NOMBRE = "nombre"
    }
}
