package com.example.simpletextview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.example.simpletextview.custom_tools.utils.ExecutionTimeAnalytic
import com.example.simpletextview.custom_tools.utils.dp
import com.example.simpletextview.simple_tv.SimpleTextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<ViewGroup>(R.id.root_view)
        val inflater = LayoutInflater.from(this)
        val count = 1

        findViewById<View>(R.id.frameLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("SimpleTextView") {
                    inflater.inflate(R.layout.frame_layout_simple, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.frame_layout_appcompat, rootView, false)
                }
            ).also(::showResult)
        }
        findViewById<View>(R.id.relativeLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("SimpleTextView") {
                    inflater.inflate(R.layout.relative_layout_simple, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.relative_layout_appcompat, rootView, false)
                }
            ).also(::showResult)
        }
        findViewById<View>(R.id.linearLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("SimpleTextView") {
                    inflater.inflate(R.layout.linear_layout_simple, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.linear_layout_appcompat, rootView, false)
                }
            ).also(::showResult)
        }
        findViewById<View>(R.id.constraintLayout).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("SimpleTextView") {
                    inflater.inflate(R.layout.constraint_layout_simple, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("AppCompatTextView") {
                    inflater.inflate(R.layout.constraint_layout_appcompat, rootView, false)
                }
            ).also(::showResult)
        }

        findViewById<View>(R.id.simple_text_view).setOnClickListener {
            ExecutionTimeAnalytic.analyzeExecutionTime(
                count,
                ExecutionTimeAnalytic.Execution("Inflate") {
                    inflater.inflate(R.layout.simple_text_view_without_cache, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("Inflate") {
                    inflater.inflate(R.layout.simple_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("Inflate") {
                    inflater.inflate(R.layout.app_compat_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("Program") {
                    SimpleTextView(this).apply {
                        text = "SimpleTextView Some Text"
                        paint.textSize = dp(20).toFloat()
                        paint.color = Color.BLACK
                        setPadding(dp(1), dp(1), dp(1), dp(1))
                        maxLines = 1
                    }
                },
                ExecutionTimeAnalytic.Execution("Program") {
                    AppCompatTextView(this).apply {
                        text = "AppCompatTextView Some Text"
                        paint.textSize = dp(20).toFloat()
                        paint.color = Color.BLACK
                        setPadding(dp(1), dp(1), dp(1), dp(1))
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