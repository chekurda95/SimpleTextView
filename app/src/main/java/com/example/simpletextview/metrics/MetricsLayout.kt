package com.example.simpletextview.metrics

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Toast

class MetricsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defAttr: Int = 0,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defAttr, defStyle) {

    private var onMeasureTimeMcs = 0L
    private var onLayoutTimeMcs = 0L
    private var measureCount = 0L
    private var layoutCount = 0L
    private var wasLogged = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (wasLogged) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val startTime = System.nanoTime()
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            val resultTime = (System.nanoTime() - startTime) / 1000
            onMeasureTimeMcs += resultTime
            measureCount++
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (wasLogged) {
            super.onLayout(changed, l, t, r, b)
        } else {
            val startTime = System.nanoTime()
            super.onLayout(changed, l, t, r, b)
            val resultTime = (System.nanoTime() - startTime) / 1000
            onLayoutTimeMcs += resultTime
            layoutCount++
            Looper.getMainLooper().queue.addIdleHandler {
                if (!wasLogged) {
                    val text = "full = ${onMeasureTimeMcs + onLayoutTimeMcs}, measure($measureCount) = $onMeasureTimeMcs, layout($layoutCount) = $onLayoutTimeMcs"
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    wasLogged = true
                }
                return@addIdleHandler false
            }
        }
    }
}