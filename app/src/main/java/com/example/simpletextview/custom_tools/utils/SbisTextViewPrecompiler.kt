package com.example.simpletextview.custom_tools.utils

import android.content.Context
import com.example.simpletextview.R
import com.example.simpletextview.simple_tv.SbisTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal object SbisTextViewPrecompiler {

    fun precompile(appContext: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            repeat(PRECOMPILE_COUNT) {
                SbisTextView(appContext, R.style.SbisTextViewPrecompile_SingleLine).precompile()
                SbisTextView(appContext, R.style.SbisTextViewPrecompile_MultiLine).precompile()
            }
        }
    }

    private fun SbisTextView.precompile() {
        measure(
            MeasureSpecUtils.makeExactlySpec(PRECOMPILE_WIDTH_PX),
            MeasureSpecUtils.makeUnspecifiedSpec()
        )
        measure(
            MeasureSpecUtils.makeAtMostSpec(PRECOMPILE_WIDTH_PX),
            MeasureSpecUtils.makeUnspecifiedSpec()
        )
        layout(0, 0, 0, 0)
    }
}

private const val PRECOMPILE_COUNT = 10
private const val PRECOMPILE_WIDTH_PX = 200