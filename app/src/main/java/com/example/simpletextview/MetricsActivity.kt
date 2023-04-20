package com.example.simpletextview

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.set
import androidx.core.view.children
import com.example.simpletextview.metrics.MetricsLayout
import com.example.simpletextview.simple_tv.SbisTextView

class MetricsActivity : AppCompatActivity() {

    private lateinit var container: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.metrics_activity)
        container = findViewById(R.id.metrics_example_container)
        findViewById<View>(R.id.metrics_button_app_compat).setOnClickListener {
            addExample(Type.TEXT_VIEW)
        }
        findViewById<View>(R.id.metrics_button_sbis_text).setOnClickListener {
            addExample(Type.NEW_SBIS_TEXT)
        }
        findViewById<View>(R.id.metrics_button_sbis_text_origin).setOnClickListener {
            addExample(Type.ORIGIN_SBIS_TEXT)
        }
    }

    private fun addExample(type: Type) {
        container.removeAllViews()

        val layoutId = when (type) {
            Type.NEW_SBIS_TEXT -> R.layout.metrics_sbis_text_layout
            Type.ORIGIN_SBIS_TEXT -> R.layout.metrics_sbis_text_layout_origin
            Type.TEXT_VIEW -> R.layout.metrics_app_compat_layout
        }

        val startInflateTime = System.nanoTime()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        val endInflateTime = System.nanoTime()
        val metricsLayout = (view as MetricsLayout)

        metricsLayout.inflateTime = (endInflateTime - startInflateTime) / 1000
        container.addView(view, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val child = metricsLayout.children.first()
        val textView = child as? TextView
        val sbisView = child as? SbisTextView
        originText = textView?.text ?: sbisView!!.text!!

        /*textView?.text = getSpannableString()
        sbisView?.text = getSpannableString()*/

        var count = 0
        findViewById<View>(R.id.metrics_button_additional).setOnClickListener {
            metricsLayout.wasLogged = false
            count += 1
            val text = when (count % 3) {
                0 -> originText
                1 -> originText
                else -> newText
            }
            textView?.text = text
            sbisView?.text = text
        }
    }

    private var originText: CharSequence = ""
    private var newText: CharSequence = "ggdfgjdzfgjdfgjfjl sgj ljsdflgjdslfgj"

    private fun getSpannableString(): CharSequence {
        val builder = SpannableStringBuilder(originText)
        builder[0, originText.length] = AbsoluteSizeSpan(60)
        return builder
    }

    enum class Type {
        NEW_SBIS_TEXT,
        ORIGIN_SBIS_TEXT,
        TEXT_VIEW
    }
}