package com.example.simpletextview.custom_tools.utils

import android.annotation.SuppressLint
import android.os.Build
import android.text.BoringLayout
import android.text.DynamicLayout
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.IntRange
import androidx.annotation.Px
import kotlin.math.ceil

class LayoutBuilder constructor(
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
    var fadingEdge: Boolean = false
) {

    private var maxLinesByParams: Int = 0
    private var layoutWidthByParams: Int = 0

    /**
     * Применить настройки [config] для создания [StaticLayout].
     */
    fun build(): Layout {
        layoutWidthByParams = getLayoutWidthByParams()
        maxLinesByParams = getMaxLinesByParams()
        return buildLayout()
    }

    /**
     * Настроить ширину текста.
     */
    private fun getLayoutWidthByParams(): Int {
        val width = width
        return when {
            width != null && width >= 0 -> width
            text is Spannable -> ceil(Layout.getDesiredWidth(text, paint)).toInt()
            else -> paint.getTextWidth(text)
        }.let { layoutWidth ->
            layoutWidth + if (isNeedFade()) ADDITIONAL_WIDTH else 0
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

    /**
     * Построить [StaticLayout] по текущим параметрам конфигуратора.
     *
     * @param isBreakHighQuality true, если необходим качественный перенос строки
     * с оптимизацией переносов строк по всему абзацу.
     */
    @SuppressLint("WrongConstant", "Range")
    private fun buildLayout(): Layout {
        val result = when {
            text is Spannable -> buildDynamic()
            boring != null -> buildBoring()
            else -> buildStaticLayout()
        }
        return result
    }

    private fun buildDynamic(): Layout =
        DynamicLayout(
            text,
            text,
            paint,
            layoutWidthByParams,
            alignment,
            spacingMulti,
            spacingAdd,
            includeFontPad,
            ellipsize,
            layoutWidthByParams
        )

    private fun buildBoring(): Layout =
        boringLayout?.replaceOrMake(
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
        ) ?: BoringLayout.make(
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

    private fun buildStaticLayout(): Layout =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, layoutWidthByParams)
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
                text.length,
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
}

private const val MAX_LINES_NO_LIMIT = Integer.MAX_VALUE
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f
private const val ELLIPSIS_CHAR = "\u2026"
private const val SINGLE_LINE = 1
private const val DEFAULT_WRAPPED_WIDTH = -1
private const val THREE_SYMBOLS_OFFSET_FROM_TELEGRAM = 3
private const val ADDITIONAL_WIDTH = 300