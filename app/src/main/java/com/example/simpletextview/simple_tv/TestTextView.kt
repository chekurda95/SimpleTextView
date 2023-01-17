package com.example.simpletextview.simple_tv

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaRecorder
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes

@SuppressLint("AppCompatCustomView")
class TestTextView private constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
    @StyleRes defStyleRes: Int = ResourcesCompat.ID_NULL,
    startTime: Long
) : TextView(context, attrs, defStyleAttr, defStyleRes) {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
        @StyleRes defStyleRes: Int = ResourcesCompat.ID_NULL,
    ) : this(context, attrs, defStyleAttr, defStyleRes, System.nanoTime())

    init {
        val resultTime = (System.nanoTime() - startTime) / 1000
        Toast.makeText(context, "TextView init time mu = $resultTime", Toast.LENGTH_LONG).show()
    }
}