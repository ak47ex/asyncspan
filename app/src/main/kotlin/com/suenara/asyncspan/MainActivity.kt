package com.suenara.asyncspan

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.io.Closeable

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private var bufferType: TextView.BufferType? = null
    private val observers = mutableListOf<Closeable>()
    private var useSpannedString = false
    private var isAnimation = false
        set(value) {
            if (field == value) return
            if (value) {
                observers.add(text_view.enableViewObserverSpan())
                observers.add(edit_text.enableViewObserverSpan())
            } else {
                observers.forEach(Closeable::close)
                observers.clear()
            }
            field = value
            setText()
        }

    val text_view: TextView
        get() = findViewById<TextView>(R.id.text_view)
    val edit_text: EditText
        get() = findViewById<EditText>(R.id.edit_text)

    val red: MaterialButton
        get() = findViewById<MaterialButton>(R.id.red)
    val blue: MaterialButton
        get() = findViewById<MaterialButton>(R.id.blue)
    val animation: MaterialButton
        get() = findViewById<MaterialButton>(R.id.animation)
    val invalidate: View
        get() = findViewById<View>(R.id.invalidate)
    val reflection: View
        get() = findViewById<View>(R.id.reflection)
    val spanwatcher: View
        get() = findViewById<View>(R.id.spanwatcher)

    val textViewColorDrawable = ColorDrawable()
    val textViewSpan = FitFontImageSpan(textViewColorDrawable)
    val textViewAnimatedSpan = FitFontImageSpan(SampleAnimationDrawable(2000L))

    val editTextColorDrawable = ColorDrawable()
    val editTextSpan = FitFontImageSpan(editTextColorDrawable)
    val editTextAnimatedSpan = FitFontImageSpan(SampleAnimationDrawable(2000L))


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButtonToggleGroup>(R.id.colors).addOnButtonCheckedListener(object :
            MaterialButtonToggleGroup.OnButtonCheckedListener {
            override fun onButtonChecked(
                group: MaterialButtonToggleGroup?,
                checkedId: Int,
                isChecked: Boolean,
            ) {
                if (!isChecked) return


                when (checkedId) {
                    red.id -> {
                        textViewColorDrawable.withoutCallback { color = Color.RED }
                        editTextColorDrawable.withoutCallback { color = Color.RED }
                        isAnimation = false
                    }

                    blue.id -> {
                        textViewColorDrawable.withoutCallback { color = Color.BLUE }
                        editTextColorDrawable.withoutCallback { color = Color.BLUE }
                        isAnimation = false
                    }

                    animation.id -> {
                        isAnimation = true
                    }
                }
            }
        })

        findViewById<MaterialButtonToggleGroup>(R.id.buffer_type).addOnButtonCheckedListener(object :
            MaterialButtonToggleGroup.OnButtonCheckedListener {
            override fun onButtonChecked(
                group: MaterialButtonToggleGroup?,
                checkedId: Int,
                isChecked: Boolean,
            ) {
                if (!isChecked) return
                when (checkedId) {
                    R.id.normal -> TextView.BufferType.NORMAL
                    R.id.spannable -> TextView.BufferType.SPANNABLE
                    R.id.editable -> TextView.BufferType.EDITABLE
                }
                setText()
            }
        })

        findViewById<MaterialButtonToggleGroup>(R.id.text_type).addOnButtonCheckedListener(object :
            MaterialButtonToggleGroup.OnButtonCheckedListener {
            override fun onButtonChecked(
                group: MaterialButtonToggleGroup?,
                checkedId: Int,
                isChecked: Boolean,
            ) {
                if (!isChecked) return
                when (checkedId) {
                    R.id.spanned_string -> useSpannedString = true
                    R.id.spannable_string -> useSpannedString = false
                }
                setText()
            }
        })


        invalidate.setOnClickListener {
            window.decorView.invalidate()
            window.decorView.requestLayout()
        }
        reflection.setOnClickListener {
            TextViewInvalidator.invalidate(text_view)
            TextViewInvalidator.invalidate(edit_text)
        }
        spanwatcher.setOnClickListener {
            text_view.invalidateSpan(textViewSpan)
            edit_text.invalidateSpan(editTextSpan)
        }

        setText()
    }


    private fun setText() {
        fun text(span: Any) = SpannableStringBuilder().apply {
            append("Hello ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                append("W", span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                val oldLen = length
                append("W")
                val newLen = length
                setSpan(span, oldLen, newLen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            append(" world!")
        }

        text_view.setText(
            if (useSpannedString) {
                SpannedString(text(if (isAnimation) textViewAnimatedSpan else textViewSpan))
            } else {
                text(if (isAnimation) textViewAnimatedSpan else textViewSpan)
            },
            bufferType
        )

        edit_text.setText(
            if (useSpannedString) {
                SpannedString(text(if (isAnimation) editTextAnimatedSpan else editTextSpan))
            } else {
                text(if (isAnimation) editTextAnimatedSpan else editTextSpan)
            },
            bufferType
        )
    }

    private inline fun <reified T : Drawable> T.withoutCallback(block: T.() -> Unit) {
        val callback = callback
        block()
        this.callback = callback
    }
}