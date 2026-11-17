package com.systemapp.daily.ui.revision

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Vista personalizada para capturar firmas dibujadas con el dedo.
 */
class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 7f // más grueso: se ve mejor en la térmica y en el PDF
    }

    private val path = Path()
    private val paths = mutableListOf<Path>()
    private var hasSignature = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        for (p in paths) {
            canvas.drawPath(p, paint)
        }
        canvas.drawPath(path, paint)

        // Línea base para la firma
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        val y = height * 0.75f
        canvas.drawLine(20f, y, width - 20f, y, linePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false // modo consulta: no firmar
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Evita que el ScrollView se desplace mientras se firma
                parent?.requestDisallowInterceptTouchEvent(true)
                path.moveTo(x, y)
                hasSignature = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                paths.add(Path(path))
                path.reset()
            }
        }

        invalidate()
        return true
    }

    fun clear() {
        paths.clear()
        path.reset()
        hasSignature = false
        invalidate()
    }

    fun isEmpty(): Boolean = !hasSignature

    /**
     * Obtiene la firma como Bitmap.
     */
    fun getSignatureBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        for (p in paths) {
            canvas.drawPath(p, paint)
        }
        canvas.drawPath(path, paint)
        return bitmap
    }

    /**
     * Firma recortada al área dibujada (con margen): así en el acta se ve grande y
     * no como un trazo pequeño en medio de un recuadro blanco.
     */
    fun getSignatureBitmapRecortada(margen: Int = 24): Bitmap {
        val completa = getSignatureBitmap()
        val limites = RectF()
        val r = RectF()
        paths.forEachIndexed { i, p ->
            p.computeBounds(r, true)
            if (i == 0) limites.set(r) else limites.union(r)
        }
        if (paths.isEmpty()) return completa
        val extra = margen + paint.strokeWidth
        val izq = (limites.left - extra).toInt().coerceIn(0, completa.width - 1)
        val arr = (limites.top - extra).toInt().coerceIn(0, completa.height - 1)
        val der = (limites.right + extra).toInt().coerceIn(izq + 1, completa.width)
        val aba = (limites.bottom + extra).toInt().coerceIn(arr + 1, completa.height)
        val recorte = Bitmap.createBitmap(completa, izq, arr, der - izq, aba - arr)
        if (recorte !== completa) completa.recycle()
        return recorte
    }
}
