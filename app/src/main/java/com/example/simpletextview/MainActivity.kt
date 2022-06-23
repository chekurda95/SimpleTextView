package com.example.simpletextview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.simpletextview.custom_tools.utils.ExecutionTimeAnalytic

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
                ExecutionTimeAnalytic.Execution("Without cache") {
                    inflater.inflate(R.layout.simple_text_view_without_cache, rootView, false)
                },
                ExecutionTimeAnalytic.Execution("With cache") {
                    inflater.inflate(R.layout.simple_text_view, rootView, false)
                },
                ExecutionTimeAnalytic.Execution {
                    inflater.inflate(R.layout.app_compat_text_view, rootView, false)
                }
            ).also(::showResult)
        }
    }

    private fun showResult(result: String) {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
    }
}