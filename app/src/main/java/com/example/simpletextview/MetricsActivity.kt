package com.example.simpletextview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity

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

        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    enum class Type {
        NEW_SBIS_TEXT,
        ORIGIN_SBIS_TEXT,
        TEXT_VIEW
    }
}