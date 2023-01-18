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
            addExample(isAppCompat = true)
        }
        findViewById<View>(R.id.metrics_button_sbis_text).setOnClickListener {
            addExample(isAppCompat = false)
        }
    }

    private fun addExample(isAppCompat: Boolean) {
        container.removeAllViews()

        val layoutId =
            if (isAppCompat) R.layout.metrics_app_compat_layout
            else R.layout.metrics_sbis_text_layout

        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }
}