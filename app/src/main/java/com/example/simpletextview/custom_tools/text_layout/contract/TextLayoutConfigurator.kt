package com.example.simpletextview.custom_tools.text_layout.contract

import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.text_layout.TextLayout
import com.example.simpletextview.custom_tools.utils.TextHighlights

/**
 * Настройка для параметров [TextLayout.TextLayoutParams].
 */
typealias TextLayoutConfig = TextLayoutConfigurator.() -> Unit

interface TextLayoutConfigurator {

    var text: CharSequence
    @get:Px var textSize: Float
    @get:ColorInt var color: Int
    var typeface: Typeface
    var ellipsize: TextUtils.TruncateAt?
    var highlights: TextHighlights?
    var alignment: Layout.Alignment

    @get:Px var layoutWidth: Int?
    @get:Px var minWidth: Int
    @get:Px var minHeight: Int
    @get:Px var maxWidth: Int?
    @get:Px var maxHeight: Int?

    var maxLines: Int
    var minLines: Int
    var maxLength: Int

    var isVisible: Boolean
    var isVisibleWhenBlank: Boolean

    var paddingStart: Int
    var paddingTop: Int
    var paddingEnd: Int
    var paddingBottom: Int

    var paint: TextPaint
    var alpha: Int
    var letterSpacing: Float

    var includeFontPad: Boolean
    var spacingAdd: Float
    var spacingMulti: Float
    var breakStrategy: Int
    var hyphenationFrequency: Int

    var needHighWidthAccuracy: Boolean
}