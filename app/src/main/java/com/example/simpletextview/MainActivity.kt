package com.example.simpletextview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.simpletextview.custom_tools.utils.ExecutionTimeAnalytic
import com.example.simpletextview.custom_tools.utils.dp
import com.example.simpletextview.simple_tv.SbisTextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<ViewGroup>(R.id.root_view)
        val inflater = LayoutInflater.from(this)
        val count = 20

        findViewById<View>(R.id.frameLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.frame_layout_appcompat, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("TextView") {
                    inflater.inflate(R.layout.frame_layout_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("SbisTextView") {
                    inflater.inflate(R.layout.frame_layout_simple, rootView, false)
                }
            ).also(::showResult)
        }
        findViewById<View>(R.id.relativeLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.relative_layout_appcompat, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("TextView") {
                    inflater.inflate(R.layout.relative_layout_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("SbisTextView") {
                    inflater.inflate(R.layout.relative_layout_simple, rootView, false)
                }
            ).also(::showResult)
        }
        findViewById<View>(R.id.linearLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.linear_layout_appcompat, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("TextView") {
                    inflater.inflate(R.layout.linear_layout_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("SbisTextView") {
                    inflater.inflate(R.layout.linear_layout_simple, rootView, false)
                }
            ).also(::showResult)
        }
        findViewById<View>(R.id.constraintLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.constraint_layout_appcompat, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("TextView") {
                    inflater.inflate(R.layout.constraint_layout_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("SbisTextView") {
                    inflater.inflate(R.layout.constraint_layout_simple, rootView, false)
                }
            ).also(::showResult)
        }

        findViewById<View>(R.id.show_fragment_with_analytic).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("Inflate") {
                    inflater.inflate(R.layout.simple_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("Inflate") {
                    inflater.inflate(R.layout.app_compat_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("Program") {
                    SbisTextView(this).apply {
                        text = "SbisTextView Some Text"
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
                        setTextColor(ContextCompat.getColor(context, R.color.black))
                        setPadding(dp(1), dp(1), dp(1), dp(1))
                        setBackgroundResource(R.drawable.ic_launcher_background)
                        maxLines = 1
                    }
                },
                ExecutionTimeAnalytic.Execution("Program") {
                    AppCompatTextView(this).apply {
                        text = "AppCompatTextView Some Text"
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
                        setTextColor(ContextCompat.getColor(context, R.color.black))
                        setPadding(dp(1), dp(1), dp(1), dp(1))
                        setBackgroundResource(R.drawable.ic_launcher_background)
                        maxLines = 1
                    }
                },
            ).also(::showResult)
        }
    }

    private fun showResult(result: String) {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
    }
}