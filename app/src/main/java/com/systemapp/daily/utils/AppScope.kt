package com.systemapp.daily.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.View
import android.view.ViewGroup
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.FileOutputStream

/**
 * Scope de proceso para escrituras cortas que no deben cancelarse al cerrar una pantalla
 * (p. ej. guardar el borrador de la revisión en onPause).
 */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)

object FotoUtils {
    /**
     * Reduce la foto de la cámara (suele venir en 12+ MP) a máx. [ladoMax] px y JPEG 80,
     * respetando la orientación EXIF. Evita OutOfMemory y subidas lentas.
     * Bloqueante: llamar en Dispatchers.IO. Si algo falla deja el archivo original.
     */
    fun comprimir(path: String, ladoMax: Int = 1600, calidad: Int = 80) {
        try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) return
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= ladoMax) sample *= 2
            val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return

            val rotacion = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val factor = minOf(1f, ladoMax.toFloat() / maxOf(decoded.width, decoded.height))
            val m = Matrix().apply { postScale(factor, factor); postRotate(rotacion) }
            val final = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
            FileOutputStream(file).use { final.compress(Bitmap.CompressFormat.JPEG, calidad, it) }
            if (final !== decoded) final.recycle()
            decoded.recycle()
        } catch (_: Throwable) {
            // Se conserva la foto original
        }
    }
}

/** Habilita/deshabilita recursivamente (modo solo consulta). */
fun View.habilitarTodo(habilitado: Boolean) {
    isEnabled = habilitado
    if (this is ViewGroup) for (i in 0 until childCount) getChildAt(i).habilitarTodo(habilitado)
}
