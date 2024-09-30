package com.suenara.asyncspan

import android.os.Build
import android.widget.TextView
import java.lang.Exception
import java.lang.reflect.Field

public object TextViewInvalidator {

    private val ignoreFail: Boolean
        get() {
            // Huawei has its own huawei.com.android.internal.widget.HwEditor
            return "HUAWEI".equals(Build.MANUFACTURER, true)
        }

    private val editorField: Field by lazy {
        TextView::class.java.getDeclaredField("mEditor").apply {
            isAccessible = true
        }
    }

    private fun invokeInvalidate(editor: Any) {
        val invalidateTextDisplayList = editor::class.java.getDeclaredMethod("invalidateTextDisplayList").apply {
            isAccessible = true
        }
        invalidateTextDisplayList(editor)
    }

    private fun getEditor(textView: TextView): Any? {
        return editorField.get(textView)
    }

    fun invalidate(textView: TextView?) {
        textView ?: return
        try {
            val editor = getEditor(textView)
            if (editor != null) {
                invokeInvalidate(editor)
            }
            textView.invalidate()
        } catch (e: Exception) {
            if (!ignoreFail) {
                // This is expensive way of invalidating RenderNodes in Editor
                val enabled = textView.isEnabled
                textView.setEnabled(!enabled)
                textView.setEnabled(enabled)
            }
        }
    }
}
