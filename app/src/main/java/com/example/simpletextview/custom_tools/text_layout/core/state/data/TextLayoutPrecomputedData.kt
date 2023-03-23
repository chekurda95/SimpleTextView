package com.example.simpletextview.custom_tools.text_layout.core.state.data

import android.text.BoringLayout

internal class TextLayoutPrecomputedData(
    val precomputedTextWidth: Int,
    val boring: BoringLayout.Metrics? = null,
    val calculatedLineLastIndex: Int? = null,
    val containsAbsoluteSizeSpans: Boolean? = null
)