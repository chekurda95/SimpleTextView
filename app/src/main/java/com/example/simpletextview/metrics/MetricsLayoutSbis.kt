package com.example.simpletextview.metrics

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.children
import com.example.simpletextview.R
import com.example.simpletextview.simple_tv.SbisTextView

class MetricsLayoutSbis @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defAttr: Int = 0,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defAttr, defStyle) {

    private var ignore: Boolean = false

    private val childView: SbisTextView

    init {
        val inflater = LayoutInflater.from(context)
        val startTime = System.nanoTime()
        inflater.inflate(R.layout.metrics_sbis_text_view, this, true)
        childView = children.first() as SbisTextView
        val endTime = System.nanoTime()
        val resultTime = (endTime - startTime) / 1000
        Statistic.addSbisInflateTime(resultTime)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val startTime = System.nanoTime()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val endTime = System.nanoTime()
        val resultTime = (endTime - startTime) / 1000
        if (ignore) return
        Statistic.addSbisContainerMeasureTime(resultTime)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val startTime = System.nanoTime()
        super.onLayout(changed, l, t, r, b)
        val endTime = System.nanoTime()
        val resultTime = (endTime - startTime) / 1000
        if (ignore) return
        Statistic.addSbisContainerLayoutTime(resultTime)
    }

    fun ignore() {
        ignore = true
        childView.ignore()
    }
}