package com.suenara.asyncspan

import android.text.SpanWatcher
import android.text.Spannable
import android.text.Spanned
import android.widget.TextView
import kotlin.collections.forEach

public fun TextView.invalidateSpan(what: Any) {
    if (!containsSpan(what)) {
        return
    }
    val spannable = text as? Spannable
    if (spannable == null) {
        if (containsSpan(what)) {
            invalidate()
        }
        return
    }

    val spanWatchers = getSpans<SpanWatcher>()
    if (spanWatchers.isEmpty()) {
        return
    }
    val start = spannable.getSpanStart(what)
    if (start == -1) {
        return
    }
    val end = spannable.getSpanEnd(what)
    if (end >= start) {
        spanWatchers.forEach { watcher ->
            watcher.onSpanChanged(spannable, what, start, end, start, end)
        }
    }
}

public fun TextView.containsSpan(span: Any): Boolean {
    return (text as? Spanned)?.getSpans(0, length(), span::class.java)?.any { it === span } == true
}

public inline fun <reified T> TextView.getSpans(): Array<out T> {
    return (text as? Spanned)?.getSpans<T>(0, text.length, T::class.java).orEmpty()
}