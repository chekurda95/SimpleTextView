package com.example.simpletextview.custom_tools.utils

import android.os.Build
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
    var containsTextSizeSpans: Boolean? = null
) {

    private var maxLinesByParams: Int = 0
    private var layoutWidthByParams: Int = 0
    private var textLength: Int = 0
    private var containsTextSizeSpansByParams = false

    private val isBoring: Boolean
        get() = boring != null &&
            ((ellipsize != null && maxLinesByParams == SINGLE_LINE) || boring!!.width <= layoutWidthByParams)

    /**
     * Применить настройки [config] для создания [StaticLayout].
     */
    fun build(): Layout {
        text = text.highlightText(highlights)
        layoutWidthByParams = getLayoutWidthByParams()
        maxLinesByParams = getMaxLinesByParams()
        containsTextSizeSpansByParams = getContainsTextSizeSpansByParams()
        textLength = getTextLengthByParams()
        return buildLayout().apply {
            tryHighlightEllipsize()
        }
    }

    /**
     * Настроить ширину текста.
     */
    private fun getLayoutWidthByParams(): Int {
        val width = width
        return when {
            width != null && width >= 0 -> width
            text is Spannable -> paint.getTextWidth(text, byLayout = true)
            else -> paint.getTextWidth(text)
        }.let { layoutWidth ->
            layoutWidth + if (isNeedFade()) ADDITIONAL_FADING_EDGE_WIDTH else 0
        }
    }

    /**
     * Настроить максимально допустимое количество строк.
     */
    private fun getMaxLinesByParams(): Int {
        val calculatedMaxLines = when {
            text.isBlank() -> SINGLE_LINE
            maxHeight != null -> maxOf(maxHeight!!, 0) / getOneLineHeight()
            else -> maxLines
        }
        return maxOf(calculatedMaxLines, SINGLE_LINE)
    }

    private fun getContainsTextSizeSpansByParams(): Boolean {
        val text = text
        val containsAbsoluteSizeSpans = containsTextSizeSpans
        return when {
            containsAbsoluteSizeSpans != null -> {
                containsAbsoluteSizeSpans
            }
            lineLastSymbolIndex != null -> {
                (text is Spannable && text.getSpans(0, text.length, AbsoluteSizeSpan::class.java).isNotEmpty())
            }
            else -> false
        }
    }

    private fun getTextLengthByParams(): Int =
        if (lineLastSymbolIndex != null && !containsTextSizeSpansByParams && maxLines != MAX_LINES_NO_LIMIT) {
            ceil(lineLastSymbolIndex!! * ONE_LINE_SYMBOLS_COUNT_RESERVE * maxLines).toInt().coerceAtMost(text.length)
        } else {
            text.length
        }

    /**
     * Построить [StaticLayout] по текущим параметрам конфигуратора.
     *
     * @param isBreakHighQuality true, если необходим качественный перенос строки
     * с оптимизацией переносов строк по всему абзацу.
     */
    private fun buildLayout(): Layout =
        if (isBoring) {
            buildBoring()
        } else {
            buildStatic()
        }

    private fun buildBoring(): BoringLayout {
        val ellipsize = ellipsize.takeIf { boring!!.width > layoutWidthByParams }
        val boringLayout = boringLayout
        return if (boringLayout != null) {
            boringLayout.replaceOrMake(
                text,
                paint,
                layoutWidthByParams,
                alignment,
                spacingMulti,
                spacingAdd,
                boring,
                includeFontPad,
                ellipsize,
                layoutWidthByParams
            )
        } else {
            BoringLayout.make(
                text,
                paint,
                layoutWidthByParams,
                alignment,
                spacingMulti,
                spacingAdd,
                boring,
                includeFontPad,
                ellipsize,
                layoutWidthByParams
            )
        }
    }


    private fun buildStatic(): StaticLayout =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, textLength, paint, layoutWidthByParams)
                .setAlignment(alignment)
                .setLineSpacing(spacingAdd, spacingMulti)
                .setIncludePad(includeFontPad)
                .setMaxLines(maxLinesByParams)
                .setBreakStrategy(breakStrategy)
                .setHyphenationFrequency(hyphenationFrequency).apply {
                    if (ellipsize != null) {
                        setEllipsize(ellipsize)
                        setEllipsizedWidth(layoutWidthByParams)
                    }
                }.build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                0,
                textLength,
                paint,
                layoutWidthByParams,
                alignment,
                spacingMulti,
                spacingAdd,
                includeFontPad,
                ellipsize,
                layoutWidthByParams
            )
        }

    private fun isNeedFade(): Boolean =
        fadingEdge && text != TextUtils.ellipsize(text, paint, layoutWidthByParams.toFloat(), TextUtils.TruncateAt.END)

    /**
     * Получить высоту одной строки текста по заданному [paint].
     */
    private fun getOneLineHeight(): Int =
        paint.textHeight.let { textHeight ->
            if (textHeight != 0 || paint.textSize == 0f) {
                textHeight
            } else {
                getOneLineHeightByStaticLayout()
            }
        }

    /**
     * Получить высоту одной строки текста путем создания [StaticLayout].
     *
     * Необходимость для тестов, где нет возможности замокать native [TextPaint],
     * который возвращает textSize == 0,
     * [StaticLayout] как-то обходит эту ситуацию через другие нативные методы.
     */
    private fun getOneLineHeightByStaticLayout(): Int {
        val paramsMaxLines = maxLines
        maxLines = 1
        return buildLayout().height.also {
            maxLines = paramsMaxLines
        }
    }

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
            this@LayoutBuilder.text.highlightText(ellipsizeHighlight)
        }
    }
}

private const val MAX_LINES_NO_LIMIT = Integer.MAX_VALUE
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f
private const val SINGLE_LINE = 1
private const val ADDITIONAL_FADING_EDGE_WIDTH = 300
private const val ONE_LINE_SYMBOLS_COUNT_RESERVE = 1.2f