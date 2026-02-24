package com.systemapp.daily.ui.firma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.systemapp.daily.R
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.UserEntity
import com.systemapp.daily.databinding.ActivityFirmaBinding
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FirmaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirmaBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirmaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnLimpiar.setOnClickListener {
            binding.signatureView.clear()
        }

        binding.btnGuardar.setOnClickListener {
            guardarFirma()
        }

        cargarFirmaActual()
    }

    private fun cargarFirmaActual() {
        val firmaPath = sessionManager.firmaPath
        if (firmaPath != null) {
            val file = File(firmaPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.ivFirmaActual.setImageBitmap(bitmap)
                    binding.cardFirmaActual.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun guardarFirma() {
        if (binding.signatureView.isEmpty()) {
            Toast.makeText(this, R.string.firma_vacia, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val bitmap = binding.signatureView.getSignatureBitmap()
                val firmaFile = withContext(Dispatchers.IO) {
                    val dir = File(filesDir, "firmas")
                    dir.mkdirs()
                    val file = File(dir, "firma_revisor_${sessionManager.userId}.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    file
                }

                sessionManager.firmaPath = firmaFile.absolutePath

                // Guardar en Room también
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(this@FirmaActivity)
                    val userId = sessionManager.userId
                    if (userId != -1) {
                        db.userDao().actualizarFirma(userId, firmaFile.absolutePath)
                    }
                }

                Toast.makeText(this@FirmaActivity, R.string.firma_guardada, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@FirmaActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
