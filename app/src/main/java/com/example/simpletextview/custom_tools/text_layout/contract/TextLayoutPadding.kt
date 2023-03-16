package com.example.simpletextview.custom_tools.text_layout.contract

import android.text.Layout
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.text_layout.TextLayout

/**
 * Параметры отступов текстовой разметки [Layout] в [TextLayout].
 */
data class TextLayoutPadding(
    @Px val start: Int = 0,
    @Px val top: Int = 0,
    @Px val end: Int = 0,
    @Px val bottom: Int = 0
)