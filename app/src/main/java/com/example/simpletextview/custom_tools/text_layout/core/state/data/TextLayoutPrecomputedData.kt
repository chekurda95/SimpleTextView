package com.example.simpletextview.custom_tools.text_layout.core.state.data

import android.text.BoringLayout

internal class TextLayoutPrecomputedData(
    val availableWidth: Int?,
    val precomputedTextWidth: Int,
    val boring: BoringLayout.Metrics? = null,
    val lineLastSymbolIndex: Int? = null,
    val hasTextSizeSpans: Boolean? = null
)