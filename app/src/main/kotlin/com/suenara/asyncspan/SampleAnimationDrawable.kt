package com.suenara.asyncspan

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.core.graphics.withRotation
import kotlin.math.cos
import kotlin.math.sin

class SampleAnimationDrawable(private val durationMs: Long) : Drawable() {

    private val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val star = Path()

    private var angle = 0.0f
        set(value) {
            field = value % 360f
        }
    private var lastFrameTime = 0L

    override fun draw(canvas: Canvas) {
        canvas.withRotation(angle, bounds.centerX().toFloat(), bounds.centerY().toFloat()) {
            drawPath(star, paint)
        }
        nextFrame()
        lastFrameTime = SystemClock.elapsedRealtime()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.setColorFilter(colorFilter)
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        star.reset()
        val radius = minOf(bounds.width(), bounds.height()) / 2f
        star.set(createStarPath(bounds.centerX(), bounds.centerY(), radius, STAR_POINTS))
    }

    fun nextFrame() {
        val actuallyElapsedMs = SystemClock.elapsedRealtime() - lastFrameTime
        val elapsedMs = if (actuallyElapsedMs > 5000L) {
            //overtime
            0L
        } else {
            actuallyElapsedMs
        }
        angle += (360f * (elapsedMs / durationMs.toDouble())).toFloat()
        invalidateSelf()
    }

    private fun createStarPath(cx: Int, cy: Int, radius: Float, points: Int): Path {
        val path = Path()
        val angle = Math.PI / points

        // Начальная точка
        path.moveTo(
            (cx + radius * cos(0.0)).toFloat(), (cy - radius * sin(0.0)).toFloat()
        )

        // Создаем звезду
        for (i in 1 until points * 2) {
            val r = (if (i % 2 == 0) radius else radius / 2.5).toFloat()
            val x = cx + r * cos(i * angle)
            val y = cy - r * sin(i * angle)
            path.lineTo(x.toFloat(), y.toFloat())
        }

        path.close() // Завершаем путь

        return path
    }

    companion object {
        private const val STAR_POINTS = 5
    }
}