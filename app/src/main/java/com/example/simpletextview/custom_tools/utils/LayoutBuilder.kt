package com.example.simpletextview.custom_tools.utils

import android.text.BoringLayout
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import androidx.annotation.IntRange
import androidx.annotation.Px
import kotlin.math.ceil

class LayoutBuilder(
    var text: CharSequence,
    var paint: TextPaint,
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
) {

    /**
     * Применить настройки [config] для создания [StaticLayout].
     */
    fun build(): Layout {
        val preparedText = text.highlightText(highlights)
        val preparedWidth = prepareWidth(preparedText, paint, width, fadingEdge)
        val preparedMaxLines = prepareMaxLines(preparedText, paint, maxLines, maxHeight)
        val preparedHasTextSizeSpans = prepareHasTextSizeSpans(preparedText, hasTextSizeSpans, lineLastSymbolIndex)
        val preparedTextLength = prepareTextLength(preparedMaxLines, preparedHasTextSizeSpans, lineLastSymbolIndex)
        return createLayout(
            text = preparedText,
            width = preparedWidth,
            textLength = preparedTextLength,
            maxLines = preparedMaxLines
        ).apply {
            tryHighlightEllipsize()
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
     * Построить [StaticLayout] по текущим параметрам конфигуратора.
     *
     * @param isBreakHighQuality true, если необходим качественный перенос строки
     * с оптимизацией переносов строк по всему абзацу.
     */
    private fun createLayout(
        text: CharSequence,
        width: Int,
        textLength: Int,
        maxLines: Int
    ): Layout =
        LayoutCreator.createLayout(
            text = text,
            paint = paint,
            width = width,
            alignment = alignment,
            length = textLength,
            spacingMulti = spacingMulti,
            spacingAdd = spacingAdd,
            includeFontPad = includeFontPad,
            maxLines = maxLines,
            breakStrategy = breakStrategy,
            hyphenationFrequency = hyphenationFrequency,
            ellipsize = ellipsize,
            boring = boring,
            boringLayout = boringLayout
        )

    /**
     * Подсветка сокращения текста при наличии [highlights] за пределами сокращения.
     */
    private fun Layout.tryHighlightEllipsize() {
        val highlights = highlights
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
            val spannableText = if (this is StaticLayout) this@LayoutBuilder.text else text
            spannableText.highlightText(ellipsizeHighlight)
        }
    }
}

private const val MAX_LINES_NO_LIMIT = Integer.MAX_VALUE
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f
private const val SINGLE_LINE = 1
private const val ADDITIONAL_FADING_EDGE_WIDTH = 300
private const val ONE_LINE_SYMBOLS_COUNT_RESERVE = 1.2f