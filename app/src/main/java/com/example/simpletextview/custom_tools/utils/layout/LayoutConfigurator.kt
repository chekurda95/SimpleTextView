package com.example.simpletextview.custom_tools.utils.layout

import android.text.BoringLayout
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import androidx.annotation.IntRange
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.utils.HighlightSpan
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import com.example.simpletextview.custom_tools.utils.TextHighlights
import com.example.simpletextview.custom_tools.utils.ellipsizeIndex
import com.example.simpletextview.custom_tools.utils.getTextWidth
import com.example.simpletextview.custom_tools.utils.highlightText
import com.example.simpletextview.custom_tools.utils.textHeight
import kotlin.math.ceil

class LayoutConfigurator {

    companion object {

        fun configure(
            config: Params.() -> Unit
        ): Layout {
            val params = Params().apply(config)
            return LayoutConfigurator().configure(params)
        }
    }

    class Params internal constructor(
        var text: CharSequence = "",
        var paint: TextPaint = SimpleTextPaint(),
        var boring: BoringLayout.Metrics? = null,
        var boringLayout: BoringLayout? = null,
        @Px var width: Int? = null,
        var alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        var ellipsize: TextUtils.TruncateAt? = TextUtils.TruncateAt.END,
        var includeFontPad: Boolean = true,
        var spacingAdd: Float = DEFAULT_SPACING_ADD,
        var spacingMulti: Float = DEFAULT_SPACING_MULTI,
        @IntRange(from = 1) var maxLines: Int = SINGLE_LINE,
        @Px var maxHeight: Int? = null,
        var highlights: TextHighlights? = null,
        var breakStrategy: Int = 0,
        var hyphenationFrequency: Int = 0,
        var fadingEdge: Boolean = false,
        var lineLastSymbolIndex: Int? = null,
        var hasTextSizeSpans: Boolean? = null
    )

    /**
     * Применить настройки [config] для создания [StaticLayout].
     */
    private fun configure(params: Params): Layout {
        val text = params.text.highlightText(params.highlights)
        val width = prepareWidth(text, params.paint, params.width, params.fadingEdge)
        val maxLines = prepareMaxLines(text, params.paint, params.maxLines, params.maxHeight)
        val hasTextSizeSpans = prepareHasTextSizeSpans(text, params.hasTextSizeSpans, params.lineLastSymbolIndex)
        val textLength = prepareTextLength(text, maxLines, hasTextSizeSpans, params.lineLastSymbolIndex)

        return LayoutCreator.createLayout(
            text = text,
            textLength = textLength,
            width = width,
            maxLines = maxLines,
            paint = params.paint,
            alignment = params.alignment,
            spacingMulti = params.spacingMulti,
            spacingAdd = params.spacingAdd,
            includeFontPad = params.includeFontPad,
            breakStrategy = params.breakStrategy,
            hyphenationFrequency = params.hyphenationFrequency,
            ellipsize = params.ellipsize,
            boring = params.boring,
            boringLayout = params.boringLayout
        ).apply {
            tryHighlightEllipsize(text, params.highlights)
        }
    }

    /**
     * Настроить ширину текста.
     */
    private fun prepareWidth(
        text: CharSequence,
        paint: TextPaint,
        width: Int?,
        fadingEdge: Boolean
    ): Int =
        when {
            width != null && width >= 0 -> width
            text is Spannable -> paint.getTextWidth(text, byLayout = true)
            else -> paint.getTextWidth(text)
        }.let { layoutWidth ->
            val additional = if (isNeedFade(text, paint, layoutWidth, fadingEdge)) ADDITIONAL_FADING_EDGE_WIDTH else 0
            layoutWidth + additional
        }

    /**
     * Настроить максимально допустимое количество строк.
     */
    private fun prepareMaxLines(
        text: CharSequence,
        paint: TextPaint,
        maxLines: Int,
        maxHeight: Int?
    ): Int {
        val calculatedMaxLines = when {
            text.isBlank() -> SINGLE_LINE
            maxHeight != null -> maxOf(maxHeight, 0) / paint.textHeight
            else -> maxLines
        }
        return maxOf(calculatedMaxLines, SINGLE_LINE)
    }

    private fun prepareHasTextSizeSpans(
        text: CharSequence,
        hasTextSizeSpans: Boolean?,
        lineLastSymbolIndex: Int?
    ): Boolean {
        return when {
            hasTextSizeSpans != null -> hasTextSizeSpans
            lineLastSymbolIndex != null -> {
                (text is Spannable && text.getSpans(0, text.length, AbsoluteSizeSpan::class.java).isNotEmpty())
            }
            else -> false
        }
    }

    private fun prepareTextLength(
        text: CharSequence,
        maxLines: Int,
        hasTextSizeSpansByParams: Boolean,
        lineLastSymbolIndex: Int?
    ): Int =
        if (lineLastSymbolIndex != null && !hasTextSizeSpansByParams && maxLines != MAX_LINES_NO_LIMIT) {
            ceil(lineLastSymbolIndex * ONE_LINE_SYMBOLS_COUNT_RESERVE * maxLines).toInt().coerceAtMost(text.length)
        } else {
            text.length
        }

    private fun isNeedFade(text: CharSequence, paint: TextPaint, width: Int, fadingEdge: Boolean): Boolean =
        fadingEdge && text != TextUtils.ellipsize(text, paint, width.toFloat(), TextUtils.TruncateAt.END)

    /**
     * Подсветка сокращения текста при наличии [highlights] за пределами сокращения.
     */
    private fun Layout.tryHighlightEllipsize(text: CharSequence, highlights: TextHighlights?) {
        if (highlights == null || highlights.positionList.isNullOrEmpty()) return

        val ellipsizeIndex = text.ellipsizeIndex ?: return
        val containsHighlightsAfterEllipsize = highlights.positionList.find { highlight ->
            highlight.start > ellipsizeIndex || highlight.end > ellipsizeIndex
        } != null

        if (containsHighlightsAfterEllipsize) {
            val span = HighlightSpan(ellipsizeIndex, ellipsizeIndex + 1)
            val ellipsizeHighlight = TextHighlights(
                positionList = listOf(span),
                highlightColor = highlights.highlightColor
            )
            val spannableText = if (this is StaticLayout) text else this.text
            spannableText.highlightText(ellipsizeHighlight)
        }
    }
}

private const val MAX_LINES_NO_LIMIT = Integer.MAX_VALUE
private const val SINGLE_LINE = 1
private const val ADDITIONAL_FADING_EDGE_WIDTH = 300
private const val ONE_LINE_SYMBOLS_COUNT_RESERVE = 1.2f
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f