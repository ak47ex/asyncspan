package com.suenara.asyncspan

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import java.io.Closeable
import java.lang.ref.WeakReference

/**
 * Span, that makes available to listen [View] attach/detach events.
 * Required to enable it with [TextView.enableViewObserverSpan]
 */
public interface ViewObserverSpan {
    public fun attach(textView: TextView)
    public fun detach(textView: TextView)
}

public fun TextView.enableViewObserverSpan(): Closeable {
    val vosl = ViewObserverSpanListener(this)
    addTextChangedListener(vosl)
    addOnAttachStateChangeListener(vosl)
    val weakVosl = WeakReference(vosl)
    val weakThis = WeakReference(this)
    return object : Closeable {
        override fun close() {
            weakThis.get()?.let { tv ->
                weakVosl.get()?.let { vosl ->
                    tv.removeTextChangedListener(vosl)
                    tv.removeOnAttachStateChangeListener(vosl)
                }
            }
        }
    }
}

private class ViewObserverSpanListener(view: TextView) : View.OnAttachStateChangeListener, TextWatcher {
    private val weakView = WeakReference(view)

    init {
        if (view.isAttachedToWindow) {
            attach(view)
        }
    }

    fun attach(view: TextView) {
        view.getSpans<ViewObserverSpan>()
            .forEach { it.attach(view) }
    }

    override fun onViewAttachedToWindow(v: View) = attach(v as TextView)

    override fun onViewDetachedFromWindow(v: View) {
        (v as TextView).getSpans<ViewObserverSpan>()
            .forEach { it.detach(v) }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) {
        weakView.get()?.let(::attach)
    }
}