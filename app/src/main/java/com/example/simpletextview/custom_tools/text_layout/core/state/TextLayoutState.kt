package com.example.simpletextview.custom_tools.text_layout.core.state

import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.style.AbsoluteSizeSpan
import androidx.annotation.FloatRange
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutBuildHelper
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.utils.getLimitedTextWidth
import com.example.simpletextview.custom_tools.utils.getTextWidth
import kotlin.math.ceil
import kotlin.math.roundToInt

internal class TextLayoutState(
    private val layoutBuildHelper: TextLayoutBuildHelper,
    val params: TextLayoutParams,
    val drawParams: TextLayoutDrawParams
) {

    /**
     * Минимальная высота текста по заданным [TextLayoutParams.minLines].
     */
    @get:Px
    private val minHeightByLines: Int by lazy(LazyThreadSafetyMode.NONE) {
        val layoutHeight = when {
            params.minLines <= 0 || !isVisible -> 0
            params.maxLines <= layout.lineCount -> layout.getLineTop(params.maxLines)
            params.minLines <= layout.lineCount -> layout.height
            else -> {
                val lineHeight = with(params) {
                    (paint.getFontMetricsInt(null) * spacingMulti + spacingAdd).roundToInt()
                }
                layout.height + (params.minLines - layout.lineCount) * lineHeight
            }
        }
        layoutHeight + params.padding.top + params.padding.bottom
    }

    /**
     * Сконфигурированный текст с учетом настроек параметров.
     */
    private val configuredText: CharSequence by lazy(LazyThreadSafetyMode.NONE) {
        with(params) {
            when {
                maxLength == Int.MAX_VALUE || maxLength < 0 -> text
                text.isEmpty() -> text
                maxLength >= text.length -> text
                else -> text.subSequence(0, maxLength)
            }
        }
    }

    /**
     * Ширина текста.
     */
    @get:Px
    private val textWidthByParams: Int by lazy(LazyThreadSafetyMode.NONE) {
        with(params) {
            layoutWidth?.let { width ->
                maxOf(width - padding.start - padding.end, 0)
            } ?: limitedTextWidth
        }
    }

    /**
     * Ширина текста с учетом ограничений.
     */
    @get:Px
    private val limitedTextWidth: Int by lazy(LazyThreadSafetyMode.NONE) {
        with(params) {
            val horizontalPadding = padding.start + padding.end
            val text = configuredText
            val containsAbsoluteSizeSpans = text is Spannable
                    && text.getSpans(0, text.length, AbsoluteSizeSpan::class.java).isNotEmpty()
            val textWidth = if (containsAbsoluteSizeSpans) {
                ceil(Layout.getDesiredWidth(text, paint)).toInt()
            } else {
                paint.getTextWidth(text)
            }
            val minTextWidth = if (minWidth > 0) maxOf(minWidth - horizontalPadding, 0) else 0
            val maxTextWidth = maxWidth?.let { maxOf(it - horizontalPadding, 0) } ?: Integer.MAX_VALUE
            maxOf(minTextWidth, minOf(textWidth, maxTextWidth))
        }
    }

    /**
     * Максимальная высота текста.
     */
    @get:Px
    private val layoutMaxHeight: Int? by lazy(LazyThreadSafetyMode.NONE) {
        with(params) {
            maxHeight?.let { maxOf(it - padding.top - padding.bottom, 0) }
        }
    }

    /**
     * Разметка, построенная по текущим параметрам [params].
     */
    val layout: Layout by lazy(LazyThreadSafetyMode.NONE) {
        layoutBuildHelper.buildLayout(
            text = configuredText,
            width = textWidthByParams,
            maxHeight = layoutMaxHeight,
            params = params
        ).also { drawParams.drawingLayout = it }
    }

    /**
     * Видимость разметки.
     */
    val isVisible: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        params.isVisible.let {
            if (!params.isVisibleWhenBlank) it && params.text.isNotBlank()
            else it
        }
    }

    /**
     * Ширина всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val width: Int by lazy(LazyThreadSafetyMode.NONE) {
        val layoutWidth = if (layout.lineCount == SINGLE_LINE && params.needHighWidthAccuracy) {
            layout.getLineWidth(0).roundToInt()
        } else {
            layout.width
        }
        params.layoutWidth
            ?: maxOf(
                params.minWidth,
                minOf(
                    params.padding.start + layoutWidth + params.padding.end,
                    params.maxWidth ?: Integer.MAX_VALUE
                )
            )
    }

    /**
     * Высота всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val height: Int by lazy(LazyThreadSafetyMode.NONE) {
        when {
            !isVisible -> 0
            width != 0 -> {
                maxOf(params.minHeight, minHeightByLines)
                    .coerceAtMost(params.maxHeight ?: Integer.MAX_VALUE)
            }
            else -> maxOf(params.minHeight, minHeightByLines)
        }
    }

    /**
     * Базовая линия текстовой разметки.
     */
    @get:Px
    val baseline: Int
        get() = params.padding.top + (drawParams.drawingLayout?.getLineBaseline(0) ?: 0)

    /**
     * Прозрачность текста разметки.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    var layoutAlpha: Float
        get() = drawParams.layoutAlpha
        set(value) {
            drawParams.layoutAlpha = value
            params.paint.alpha = (value * drawParams.textColorAlpha).toInt()
        }

    /**
     * Получить ожидаемую ширину разметки для однострочного текста [text] без создания [StaticLayout].
     * По-умолчанию используется текст из параметров рамзетки [TextLayoutParams.text].
     */
    @Px
    fun getDesiredWidth(
        text: CharSequence? = null,
        maxWidth: Int? = null
    ): Int = with(params) {
        val resultText = text ?: this.text
        val horizontalPadding = padding.start + padding.end
        if (maxWidth != null) {
            val (width, lastIndex) = params.paint.getLimitedTextWidth(resultText, maxWidth - horizontalPadding)
            if (text == null) layoutBuildHelper.lineLastIndex = lastIndex
            width
        } else {
            paint.getTextWidth(resultText) + horizontalPadding
        }
    }

    /**
     * Получить ожидаемую высоту разметки для однострочного текста без создания [StaticLayout].
     */
    @Px
    fun getDesiredHeight(): Int = with(params) {
        val fm = paint.fontMetrics
        (fm.bottom - fm.top + fm.leading).roundToInt() + padding.top + padding.bottom
    }


    /**
     * Измерить ширину разметки с учетом ограничений:
     * - [TextLayoutParams.maxWidth]
     * - [TextLayoutParams.minWidth]
     * - [TextLayoutParams.maxLength]
     */
    @Px
    fun measureWidth(): Int = with(params.padding) {
        start + end + limitedTextWidth
    }
}

private const val SINGLE_LINE = 1