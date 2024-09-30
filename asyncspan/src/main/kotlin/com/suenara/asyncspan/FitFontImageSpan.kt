package com.suenara.asyncspan

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import androidx.core.view.doOnPreDraw
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Спан для изображений, которые используют размер шрифта. Можно использовать разные стратегии [ScaleType],
 * чтобы вписать [drawable] в квадрат со стороной равной высоте шрифта (descent - ascent).
 *
 * Наследование от [com.suenara.asyncspan.ViewObserverSpan] требуется для корректной работы [Drawable.Callback], в противном случае,
 * в той же фреске после загрузки изображения спан не обновится и будет пустое место в тексте.
 */
public open class FitFontImageSpan(
    drawable: Drawable,
    private val scaleType: ScaleType = ScaleType.CENTER_INSIDE,
) : ImageSpan(drawable), ViewObserverSpan {

    private val fontRect = RectF()
    private val tempRect = RectF()
    private val drawableRect = Rect()

    private var lastAttachedView = WeakReference<TextView>(null)
    private var callback: SpanDrawableCallback? = null

    override fun attach(textView: TextView) {
        if (lastAttachedView.get() == textView) return

        lastAttachedView = WeakReference(textView)
        drawable.callback = SpanDrawableCallback(lastAttachedView).also {
            callback = it
        }
    }

    override fun detach(textView: TextView) {
        lastAttachedView.clear()
        drawable.callback = null
        callback = null
    }

    public override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fontMetricsInt: Paint.FontMetricsInt?,
    ): Int {
        val drawable = drawable
        if (fontMetricsInt != null) {
            val fmPaint = paint.fontMetricsInt
            val fontHeight = fmPaint.descent - fmPaint.ascent
            fontRect.set(0f, 0f, fontHeight.toFloat(), fontHeight.toFloat())
            tempRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            scaleRect(tempRect, fontRect, drawableRect, scaleType)
            drawable.bounds = drawableRect
        }
        return fontRect.right.toInt()
    }

    public override fun draw(
        canvas: Canvas, text: CharSequence, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint,
    ) {
        canvas.withTranslation(x, top.toFloat()) {
            canvas.withClip(fontRect) {
                drawable.draw(canvas)
            }
        }
    }

    private fun scaleRect(src: RectF, dst: RectF, out: Rect, scaleType: ScaleType) {
        val srcWidth = if (src.width() == -1f) dst.width() else src.width()
        val srcHeight = if (src.height() == -1f) dst.height() else src.height()

        return when (scaleType) {
            ScaleType.FIT_XY -> {
                out.set(
                    floor(dst.left).toInt(),
                    floor(dst.top).toInt(),
                    ceil(dst.right).toInt(),
                    ceil(dst.bottom).toInt(),
                )
            }

            ScaleType.FILL -> {
                val srcAspect = srcWidth / srcHeight
                val newWidth: Float
                val newHeight: Float
                if (srcAspect > 1) {
                    newWidth = dst.height() * srcAspect
                    newHeight = dst.height()
                } else {
                    newWidth = dst.width()
                    newHeight = dst.width() / srcAspect
                }
                val left = dst.left + (dst.width() - newWidth) / 2
                val top = dst.top + (dst.height() - newHeight) / 2

                out.set(
                    floor(left).toInt(),
                    floor(top).toInt(),
                    ceil(left + newWidth).toInt(),
                    ceil(top + newHeight).toInt(),
                )
            }

            ScaleType.CENTER_INSIDE -> {
                val srcAspect = srcWidth / srcHeight
                val dstAspect = dst.width() / dst.height()
                val newWidth: Float
                val newHeight: Float
                if (srcAspect > dstAspect) {
                    newWidth = dst.width()
                    newHeight = dst.width() / srcAspect
                } else {
                    newHeight = dst.height()
                    newWidth = dst.height() * srcAspect
                }
                val left = dst.left + (dst.width() - newWidth) / 2
                val top = dst.top + (dst.height() - newHeight) / 2

                out.set(
                    floor(left).toInt(),
                    floor(top).toInt(),
                    ceil(left + newWidth).toInt(),
                    ceil(top + newHeight).toInt(),
                )
            }

            ScaleType.CENTER -> {
                val left = dst.left + (dst.width() - srcWidth) / 2
                val top = dst.top + (dst.height() - srcHeight) / 2

                out.set(
                    floor(left).toInt(),
                    floor(top).toInt(),
                    ceil(left + srcWidth).toInt(),
                    ceil(top + srcHeight).toInt(),
                )
            }
        }
    }

    private inner class SpanDrawableCallback(private val ref: WeakReference<TextView>) : Drawable.Callback {

        private val isInvalidatePosted = AtomicBoolean(false)

        override fun invalidateDrawable(who: Drawable) {
            if (isInvalidatePosted.compareAndSet(false, true)) {
                val weakTextView = ref.get() ?: return
                val isSpanRemoved = !weakTextView.containsSpan(this@FitFontImageSpan)
                if (isSpanRemoved) {
                    detach(weakTextView)
                    return
                }
                weakTextView.doOnPreDraw {
                    weakTextView.invalidateSpan(this@FitFontImageSpan)
                    isInvalidatePosted.set(false)
                }
            }
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            ref.get()?.postDelayed(what, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            ref.get()?.removeCallbacks(what)
        }
    }

    public enum class ScaleType {
        /**
         * Масштабирует исходный Rect так, чтобы его размеры совпадали
         * с целевым Rect'ом, при этом исходный может быть искажён, так как пропорции не сохраняются.
         */
        FIT_XY,

        /**
         * Масштабирует исходный Rect так, чтобы он заполнил целевой Rect
         * с сохранением пропорций. Rect может быть обрезан, если его пропорции не совпадают с целевыми.
         */
        FILL,

        /**
         * Масштабирует исходный Rect так, чтобы он полностью поместился
         * в целевой Rect, сохраняя пропорции. Если исходный Rect меньше целевого,
         * он будет отцентрирован без масштабирования.
         */
        CENTER_INSIDE,

        /**
         * Размещает исходный Rect в центре целевого без изменения его размеров.
         * Если исходный Rect больше целевого, часть его содержимого может выйти за границы.
         */
        CENTER
    }

}