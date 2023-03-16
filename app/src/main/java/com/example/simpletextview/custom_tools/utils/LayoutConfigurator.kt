package com.example.simpletextview.custom_tools.utils

import android.annotation.SuppressLint
import android.os.Build
import android.text.BoringLayout
import android.text.DynamicLayout
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.Px
import kotlin.math.ceil

class LayoutConfigurator constructor(
    private var text: CharSequence,
    private var paint: TextPaint,
    private var boring: BoringLayout.Metrics? = null,
    private var savedLayout: BoringLayout? = null,
    @Px var width: Int = DEFAULT_WRAPPED_WIDTH,
    var alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    var ellipsize: TextUtils.TruncateAt? = TextUtils.TruncateAt.END,
    var includeFontPad: Boolean = true,
    var spacingAdd: Float = DEFAULT_SPACING_ADD,
    var spacingMulti: Float = DEFAULT_SPACING_MULTI,
    @IntRange(from = 1) var maxLines: Int = SINGLE_LINE,
    @Px var maxHeight: Int? = null,
    var highlights: TextHighlights? = null,
    var canContainUrl: Boolean = false,
    var breakStrategy: Int = 0,
    var hyphenationFrequency: Int = 0,
    var fadingEdge: Boolean = false
) {

    /**
     * Применить настройки [config] для создания [StaticLayout].
     */
    internal fun configure(): Layout {
        configureMaxLines()
        configureWidth()
        return buildLayout()
           /* .let { layout ->
            if (layout.lineCount <= maxLines) {

            } else {
                val lastLineIndex = maxLines - 1
                val lineWidth = layout.getLineWidth(lastLineIndex)
                val off = layout.getOffsetForHorizontal(lastLineIndex, lineWidth)
                val endIndex = maxOf(if (lineWidth < width) off else off - THREE_SYMBOLS_OFFSET_FROM_TELEGRAM, 0)
                text = SpannableStringBuilder(text.subSequence(0, endIndex))
                    .apply { if (!text.hasSymbolEllipsize && ellipsize == TextUtils.TruncateAt.END) append(ELLIPSIS_CHAR) }
                buildLayout(canContainUrl)
            }
        }*/
    }

    /**
     * Настроить ширину текста.
     */
    private fun configureWidth() {
        width = when {
            text !is Spannable -> {
                maxOf(
                    width.takeIf { it != DEFAULT_WRAPPED_WIDTH }
                        ?: paint.getTextWidth(text),
                    0
                )
            }
            width == DEFAULT_WRAPPED_WIDTH -> {
                ceil(Layout.getDesiredWidth(text, paint)).toInt()
            }
            else -> {
                maxOf(0, width)
            }
        }
    }

    /**
     * Настроить максимально допустимое количество строк.
     */
    private fun configureMaxLines() {
        val calculatedMaxLines = when {
            text.isBlank() -> SINGLE_LINE
            maxHeight != null -> maxOf(maxHeight!!, 0) / getOneLineHeight()
            else -> maxLines
        }
        maxLines = maxOf(calculatedMaxLines, SINGLE_LINE)
    }

    /**
     * Настроить и вернуть текст.
     * При необходимости сокращает и подсвечивает текст по параметрам [highlights].
     */
    private fun configureText() {
        if (isNeedFade()) return
        text = when {
            // Текст без ограничений по высоте или ширине -> не пытаемся сокращать
            maxLines == MAX_LINES_NO_LIMIT -> {
                text.highlightText(highlights)
            }
            // Spannable текст с переносами с ограничением в maxLines -> парсим строки руками и при необходимости сокращаем
            maxLines > 0 && text.contains("\n") -> {
                if (text !is Spannable) {
                    text.split("\n")
                        .joinToString(separator = "\n", limit = maxLines)
                        .ellipsizeAndHighlightText(paint, width * maxLines, highlights, ellipsize)
                } else {
                    var lastHyphenationIndex = 0
                    repeat(maxLines) {
                        val index = text.indexOf("\n", lastHyphenationIndex + 1, false)
                        lastHyphenationIndex = index
                        if (index != -1) {
                            return@repeat
                        }
                    }
                    if (lastHyphenationIndex != -1) {
                        text.subSequence(0, lastHyphenationIndex)
                    } else {
                        text
                    }.ellipsizeAndHighlightText(paint, width * maxLines, highlights, ellipsize)
                }
            }
            // Многострочный текст с ограничением по ширине -> при необходимости сокращаем
            else -> {
                text
                //text.ellipsizeAndHighlightText(paint, width * maxLines, highlights, ellipsize)
            }
        }
    }

    // mPrecomputed see useDynamicLayout
    private fun useDynamicLayout(): Boolean =
        text is Spannable

    /**
     * Построить [StaticLayout] по текущим параметрам конфигуратора.
     *
     * @param isBreakHighQuality true, если необходим качественный перенос строки
     * с оптимизацией переносов строк по всему абзацу.
     */
    @SuppressLint("WrongConstant", "Range")
    private fun buildLayout(): Layout {
        val calculatedWidth = width + if (isNeedFade()) ADDITIONAL_WIDTH else 0
        val result = when {
            useDynamicLayout() -> buildDynamic(calculatedWidth)
            boring != null && boring!!.width <= calculatedWidth -> buildBoring(calculatedWidth)
            else -> buildStaticLayout(calculatedWidth)
        }
        return result
    }

    private fun buildDynamic(width: Int): Layout =
        DynamicLayout(
            text,
            text,
            paint,
            width,
            alignment,
            spacingMulti,
            spacingAdd,
            includeFontPad,
            ellipsize,
            width
        )

    private fun buildBoring(width: Int): Layout =
        savedLayout?.replaceOrMake(
            text,
            paint,
            width,
            alignment,
            spacingMulti,
            spacingAdd,
            boring,
            includeFontPad,
            ellipsize,
            width
        ) ?: BoringLayout.make(
            text,
            paint,
            width,
            alignment,
            spacingMulti,
            spacingAdd,
            boring,
            includeFontPad,
            ellipsize,
            width
        )

    private fun buildStaticLayout(width: Int): Layout =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(alignment)
                .setLineSpacing(spacingAdd, spacingMulti)
                .setIncludePad(includeFontPad)
                .setMaxLines(maxLines)
                .setBreakStrategy(breakStrategy)
                .setHyphenationFrequency(hyphenationFrequency)
            if (ellipsize != null) {
                builder.setEllipsize(ellipsize)
                    .setEllipsizedWidth(width)
            }
            builder.build()
        } else {
            @Suppress("DEPRECATION")
            (StaticLayout(
                text,
                0,
                text.length,
                paint,
                width,
                alignment,
                spacingMulti,
                spacingAdd,
                includeFontPad,
                ellipsize,
                width
            ))
        }

    private fun isNeedFade(): Boolean =
        fadingEdge && text != TextUtils.ellipsize(text, paint, width.toFloat(), TextUtils.TruncateAt.END)

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
}

private typealias LayoutConfig = LayoutConfigurator.() -> Unit

private const val MAX_LINES_NO_LIMIT = Integer.MAX_VALUE
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f
private const val ELLIPSIS_CHAR = "\u2026"
private const val SINGLE_LINE = 1
private const val DEFAULT_WRAPPED_WIDTH = -1
private const val THREE_SYMBOLS_OFFSET_FROM_TELEGRAM = 3
private const val ADDITIONAL_WIDTH = 300