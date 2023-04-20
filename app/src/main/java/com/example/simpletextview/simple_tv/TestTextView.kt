package com.example.simpletextview.simple_tv

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.example.simpletextview.metrics.Statistic

@SuppressLint("AppCompatCustomView")
class TestTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = android.R.attr.textViewStyle,
    @StyleRes defStyleRes: Int = ResourcesCompat.ID_NULL
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val startTime = System.nanoTime()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val resultTime = (System.nanoTime() - startTime) / 1000
        Statistic.addCompatMeasureTime(resultTime)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val startTime = System.nanoTime()
        super.onLayout(changed, left, top, right, bottom)
        val resultTime = (System.nanoTime() - startTime) / 1000
        Statistic.addCompatLayoutTime(resultTime)
    }
}