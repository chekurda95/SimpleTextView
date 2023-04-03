package com.example.simpletextview

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletextview.custom_tools.utils.MeasureSpecUtils
import com.example.simpletextview.custom_tools.utils.layout
import com.example.simpletextview.metrics.Statistic

class MetricsActivity2 : AppCompatActivity() {

    private lateinit var container: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.metrics_activity_2)
        container = findViewById(R.id.metrics_example_container)
        Looper.getMainLooper().queue.addIdleHandler {
            repeat(10) {
                val view = LayoutInflater.from(this).inflate(R.layout.metrics_2_sbis_text_layout, container, false)
                view.measure(MeasureSpecUtils.makeExactlySpec(100), MeasureSpecUtils.makeUnspecifiedSpec())
                view.measure(MeasureSpecUtils.makeAtMostSpec(50), MeasureSpecUtils.makeUnspecifiedSpec())
                view.layout(0, 0)
            }
            true
        }

        findViewById<View>(R.id.metrics_button_app_compat).setOnClickListener {
            addExample(Type.TEXT_VIEW)
        }
        findViewById<View>(R.id.metrics_button_sbis_text).setOnClickListener {
            addExample(Type.NEW_SBIS_TEXT)
        }
        findViewById<View>(R.id.metrics_button_statistic).setOnClickListener {
            container.removeAllViews()
            findViewById<TextView>(R.id.statistic_text).text = Statistic.getStatistic()
        }
    }

    private fun addExample(type: Type) {
        container.removeAllViews()

        val layoutId = when (type) {
            Type.NEW_SBIS_TEXT -> R.layout.metrics_2_sbis_text_layout
            Type.TEXT_VIEW -> R.layout.metrics_2_compat_text_layout
        }

        val startInflateTime = System.nanoTime()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        val endInflateTime = System.nanoTime()
        val inflateTime = (endInflateTime - startInflateTime) / 1000
        if (type == Type.TEXT_VIEW) {
            Statistic.addCompatContainerInflateTime(inflateTime)
        } else {
            Statistic.addSbisContainerInflateTime(inflateTime)
        }
        container.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT))


    }

    enum class Type {
        NEW_SBIS_TEXT,
        TEXT_VIEW
    }
}