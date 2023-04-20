package com.example.simpletextview.custom_tools.utils.layout

import android.text.BoringLayout
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.MetricAffectingSpan
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.utils.HighlightSpan
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import com.example.simpletextview.custom_tools.utils.TextHighlights
import com.example.simpletextview.custom_tools.utils.ellipsizeIndex
import com.example.simpletextview.custom_tools.utils.getTextWidth
import com.example.simpletextview.custom_tools.utils.highlightText
import com.example.simpletextview.custom_tools.utils.textHeight
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.EMPTY
import kotlin.math.ceil

/**
 * Конфигуратор для создания текстовой разметки [Layout] на базе [StaticLayout] или [BoringLayout].
 * @see createLayout
 *
 * Является модернизированной оберткой над [StaticLayout.Builder] и [BoringLayout.make] и служит для упрощения создания
 * однострочной и многострочной текстовой разметки, скрывая алгоритмы подбора аргументов.
 * Также имеет расширенные возможности, например:
 * @see [Params.highlights]
 * @see [Params.maxHeight]
 * @see [Params.fadingEdgeSize]
 *
 * @author vv.chekurda
 */
object LayoutConfigurator {

    /**
     * Создать текстовую разметку.
     *
     * @param text текст, который будет находиться в [StaticLayout].
     * @param paint краска, которой будет рисоваться текст [text].
     * @param config конфигурация для создания [Layout].
     */
    fun createLayout(text: CharSequence, paint: TextPaint, config: (Params.() -> Unit)? = null): Layout {
        val params = Params().apply { config?.invoke(this) }
        return createLayout(text, paint, params)
    }

    /**
     * Параметры конфигурации для построения [Layout].
     *
     * @property boring метрики для создания [BoringLayout].
     * Высчитывать и передавать для короткого текста не [Spannable] текста, чтобы получить прирост производительности.
     * @property boringLayout ранее созданный [BoringLayout] для возможности модификации уже существующего объекта.
     * Можно хранить на внешнем уровне после каждого [createLayout], если [Layout] это [BoringLayout],
     * и использовать при повторном вызове для ускорения создания [BoringLayout].
     * @property width ширина контейнера текста. По-умолчанию ширина текста.
     * @property alignment мод выравнивания текста. По-умолчанию выравнивание по левому краю.
     * @property ellipsize мод сокращения текста. По-умолчанию текст сокращается в конце.
     * @property includeFontPad true, если необходимо учитывать отступы шрифта, аналог атрибута includeFontPadding.
     * @property spacingAdd величина межстрочного интервала.
     * @property spacingMulti множитель межстрочного интервала.
     * @property maxLines максимально допустимое количество строк, аналогично механике [TextView.setMaxLines].
     * Null - без ограничений.
     * @property isSingleLine true, если при использовании [boring] необходимо создать именно [BoringLayout].
     * Логика кажется странной, но у [TextView] на singleLine безусловно создается [BoringLayout],
     * а с maxLines == 1 только в случае, когда ширина текста меньше [width].
     * @property maxHeight максимально допустимая высота. Опционально необходима для ограничения
     * количества строк высотой, а не значением.
     * @property highlights модель для выделения текста, например для сценариев поиска.
     * @property breakStrategy стратегия разрыва строки, см [Layout.BREAK_STRATEGY_SIMPLE].
     * @property hyphenationFrequency частота переноса строк, см. [Layout.HYPHENATION_FREQUENCY_NONE].
     * @property fadingEdgeSize построение разметки с учетом возможного размыливания вместо сокращения текста.
     * @property lineLastSymbolIndex (опционально, для оптимизации) индекс последнего символа
     * в строке из [TextPaint.getTextWidth].
     * Используется для отсечения текста, который будет находиться за пределами видимости.
     * @property hasMetricAffectingSpan (опционально, для оптимизации) признак того, что в тексте есть спаны,
     * влияющие на ширину строки. В случае null при необходимости этот признак будет получен при построении.
     */
    class Params internal constructor(
        var boring: BoringLayout.Metrics? = null,
        var boringLayout: BoringLayout? = null,
        @Px var width: Int? = null,
        var alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        var ellipsize: TextUtils.TruncateAt? = TextUtils.TruncateAt.END,
        var includeFontPad: Boolean = true,
        var spacingAdd: Float = DEFAULT_SPACING_ADD,
        var spacingMulti: Float = DEFAULT_SPACING_MULTI,
        @IntRange(from = 1) var maxLines: Int = SINGLE_LINE,
        var isSingleLine: Boolean = false,
        @Px var maxHeight: Int? = null,
        var highlights: TextHighlights? = null,
        var breakStrategy: Int = 0,
        var hyphenationFrequency: Int = 0,
        var fadingEdgeSize: Int = 0,
        var lineLastSymbolIndex: Int? = null,
        var hasMetricAffectingSpan: Boolean? = null
    )

    /**
     * Применить настройки [params] для создания [Layout].
     */
    private fun createLayout(text: CharSequence, paint: TextPaint, params: Params): Layout {
        val resultText = text.highlightText(params.highlights)
        val width = prepareWidth(text, paint, params.width, params.fadingEdgeSize)
        val maxLines = prepareMaxLines(text, paint, params.isSingleLine, params.maxLines, params.maxHeight)
        val hasMetricAffectingSpan = prepareHasMetricsAffectingSpans(text, params.hasMetricAffectingSpan, params.lineLastSymbolIndex)
        val textLength = prepareTextLength(text, maxLines, hasMetricAffectingSpan, params.lineLastSymbolIndex)

        return LayoutCreator.createLayout(
            text = resultText,
            textLength = textLength,
            width = width,
            maxLines = maxLines,
            paint = paint,
            isSingleLine = params.isSingleLine,
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
            tryHighlightEllipsize(resultText, params.highlights)
        }
    }

    /**
     * Настроить ширину текста.
     */
    private fun prepareWidth(
        text: CharSequence,
        paint: TextPaint,
        width: Int?,
        fadingEdgeSize: Int
    ): Int =
        when {
            width == null -> paint.getTextWidth(text, byLayout = text is Spannable)
            width >= 0 -> width
            else -> 0
        }.let { layoutWidth ->
            val additional = if (isNeedFade(text, paint, layoutWidth, fadingEdgeSize > 0)) fadingEdgeSize else 0
            layoutWidth + additional
        }

    /**
     * Настроить максимально допустимое количество строк.
     */
    private fun prepareMaxLines(
        text: CharSequence,
        paint: TextPaint,
        isSingleLine: Boolean,
        maxLines: Int,
        maxHeight: Int?
    ): Int {
        val calculatedMaxLines = when {
            text.isBlank() || isSingleLine -> SINGLE_LINE
            maxHeight != null -> maxOf(maxHeight, 0) / getOneLineHeight(paint)
            else -> maxLines
        }
        return maxOf(calculatedMaxLines, SINGLE_LINE)
    }

    private fun prepareHasMetricsAffectingSpans(
        text: CharSequence,
        hasMetricAffectingSpan: Boolean?,
        lineLastSymbolIndex: Int?
    ): Boolean =
        when {
            hasMetricAffectingSpan != null -> hasMetricAffectingSpan
            lineLastSymbolIndex != null -> {
                (text is Spannable && text.getSpans(0, text.length, MetricAffectingSpan::class.java).isNotEmpty())
            }
            else -> false
        }

    private fun prepareTextLength(
        text: CharSequence,
        maxLines: Int,
        hasMetricAffectingSpan: Boolean,
        lineLastSymbolIndex: Int?
    ): Int =
        if (lineLastSymbolIndex != null && !hasMetricAffectingSpan && maxLines != MAX_LINES_NO_LIMIT) {
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

    private fun getOneLineHeight(paint: TextPaint): Int {
        val textHeight = paint.textHeight
        return if (textHeight > 0 || paint.textSize == 0f) {
            textHeight
        } else {
            // Test case
            LayoutCreator.createLayout(text = "1", paint = paint, width = 0).height
        }
    }
}

private const val MAX_LINES_NO_LIMIT = Integer.MAX_VALUE
private const val SINGLE_LINE = 1
private const val ONE_LINE_SYMBOLS_COUNT_RESERVE = 1.2f
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f