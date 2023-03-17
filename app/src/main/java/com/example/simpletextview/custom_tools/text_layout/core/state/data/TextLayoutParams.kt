package com.example.simpletextview.custom_tools.text_layout.core.state.data

import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import com.example.simpletextview.custom_tools.utils.TextHighlights
import org.apache.commons.lang3.StringUtils

internal data class TextLayoutParams(
    val text: CharSequence = StringUtils.EMPTY,
    val paint: TextPaint = SimpleTextPaint(),
    @Px val layoutWidth: Int? = null,
    val alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    val ellipsize: TextUtils.TruncateAt? = TextUtils.TruncateAt.END,
    val includeFontPad: Boolean = true,
    val spacingAdd: Float = DEFAULT_SPACING_ADD,
    val spacingMulti: Float = DEFAULT_SPACING_MULTI,
    val maxLines: Int = SINGLE_LINE,
    val minLines: Int = 0,
    val maxLength: Int = Int.MAX_VALUE,
    @Px val minWidth: Int = 0,
    @Px val minHeight: Int = 0,
    @Px val maxWidth: Int? = null,
    @Px val maxHeight: Int? = null,
    val isVisible: Boolean = true,
    val isVisibleWhenBlank: Boolean = true,
    val padding: TextLayoutPadding = TextLayoutPadding(),
    val highlights: TextHighlights? = null,
    val breakStrategy: Int = 0,
    val hyphenationFrequency: Int = 0,
    val needHighWidthAccuracy: Boolean = false
)

private const val SINGLE_LINE = 1
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f