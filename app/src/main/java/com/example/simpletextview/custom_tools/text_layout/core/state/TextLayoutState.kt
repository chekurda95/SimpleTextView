package com.example.simpletextview.custom_tools.text_layout.core.state

import android.text.BoringLayout
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.style.AbsoluteSizeSpan
import androidx.annotation.FloatRange
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutBuildHelper
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutFadingEdgeHelper
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutPrecomputedData
import com.example.simpletextview.custom_tools.utils.getTextWidth
import kotlin.math.roundToInt

internal class TextLayoutState(
    private val layoutBuildHelper: TextLayoutBuildHelper,
    private val fadingEdgeHelper: TextLayoutFadingEdgeHelper,
    val params: TextLayoutParams,
    val drawParams: TextLayoutDrawParams
) {

    /**
     * Сконфигурированный текст с учетом настроек параметров.
     */
    private val text: CharSequence by lazy(stateLazyMode) {
        with(params) {
            when {
                maxLength == Int.MAX_VALUE || maxLength < 0 -> text
                text.isEmpty() -> text
                maxLength >= text.length -> text
                else -> text.subSequence(0, maxLength)
            }
        }
    }

    private val horizontalPadding: Int by lazy(stateLazyMode) {
        params.padding.start + params.padding.end
    }

    private val verticalPadding: Int by lazy(stateLazyMode) {
        params.padding.top + params.padding.bottom
    }

    /**
     * Ширина текста.
     */
    @get:Px
    private val textWidth: Int by lazy(stateLazyMode) {
        with(params) {
            layoutWidth?.let { width ->
                maxOf(width - horizontalPadding, 0)
            } ?: limitedTextWidth
        }
    }

    @get:Px
    private val maxTextWidth: Int by lazy(stateLazyMode) {
        with(params) {
            maxWidth?.let { maxOf(it - horizontalPadding, 0) } ?: Integer.MAX_VALUE
        }
    }

    @get:Px
    private val minTextWidth: Int by lazy(stateLazyMode) {
        with(params) {
            if (minWidth > 0) maxOf(minWidth - horizontalPadding, 0) else 0
        }
    }

    /**
     * Ширина текста с учетом ограничений.
     */
    @get:Px
    private val limitedTextWidth: Int by lazy(stateLazyMode) {
        with(params) {
            val precomputedData = getPrecomputedData()
            layoutBuildHelper.precomputedData = precomputedData
            precomputedData.precomputedTextWidth
        }
    }

    /**
     * Минимальная высота текста по заданным [TextLayoutParams.minLines].
     */
    @get:Px
    private val minHeightByLines: Int by lazy(stateLazyMode) {
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
        layoutHeight + verticalPadding
    }

    /**
     * Максимальная высота текста.
     */
    @get:Px
    private val layoutMaxHeight: Int? by lazy(stateLazyMode) {
        with(params) {
            maxHeight?.let { maxOf(it - verticalPadding, 0) }
        }
    }

    /**
     * Разметка, построенная по текущим параметрам [params].
     */
    val layout: Layout by lazy(stateLazyMode) {
        with(layoutBuildHelper) {
            if (precomputedData == null) {
                precomputedData = getPrecomputedData(textWidth)
            }
            buildLayout(
                text = text,
                width = textWidth,
                maxHeight = layoutMaxHeight,
                fadingEdge = fadingEdgeHelper.useFadingEdgeForLayout,
                params = params
            ).also {
                drawParams.drawingLayout = it
                fadingEdgeHelper.updateFadeEdgeVisibility(textWidth, params)
            }
        }
    }

    /**
     * Видимость разметки.
     */
    val isVisible: Boolean by lazy(stateLazyMode) {
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
    val width: Int by lazy(stateLazyMode) {
        if (params.layoutWidth != null) {
            params.layoutWidth
        } else {
            val layoutWidth = if (layout.lineCount == SINGLE_LINE && params.needHighWidthAccuracy) {
                layout.getLineWidth(0).roundToInt()
            } else {
                layout.width
            }
            (layoutWidth + horizontalPadding)
                .coerceAtMost(params.maxWidth ?: Integer.MAX_VALUE)
                .coerceAtLeast(params.minWidth)
        }
    }

    /**
     * Высота всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val height: Int by lazy(stateLazyMode) {
        if (isVisible) {
            params.minHeight
                .coerceAtLeast(minHeightByLines)
                .coerceAtMost(params.maxHeight ?: Integer.MAX_VALUE)
        } else {
            0
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
    fun getDesiredWidth(text: CharSequence? = null): Int =
        horizontalPadding + params.paint.getTextWidth(text ?: this.text)

    /**
     * Получить ожидаемую высоту разметки для однострочного текста без создания [StaticLayout].
     */
    @Px
    fun getDesiredHeight(): Int =
        with(params.paint.fontMetrics) {
            (bottom - top + leading).roundToInt() + verticalPadding
        }

    @Px
    fun getPrecomputedWidth(availableWidth: Int? = null): Int {
        val textWidth = if (availableWidth == null) {
            limitedTextWidth
        } else {
            val precomputedData = getPrecomputedData(availableWidth)
            layoutBuildHelper.precomputedData = precomputedData
            precomputedData.precomputedTextWidth
        }
        return textWidth + horizontalPadding
    }

    private fun getPrecomputedData(availableWidth: Int? = null): TextLayoutPrecomputedData {
        val text = text
        val availableTextWidth = (availableWidth ?: Int.MAX_VALUE) - horizontalPadding
        val limitedTextWidth = availableTextWidth.coerceAtMost(maxTextWidth)
        val isWrappedWidth = availableWidth == null && params.maxWidth == null

        val hasTextSizeSpans = text is Spannable
            && text.getSpans(0, text.length, AbsoluteSizeSpan::class.java).isNotEmpty()

        var isBoring: BoringLayout.Metrics? = null
        var lineLastSymbolIndex: Int? = null

        if (text !is Spannable && text.length <= 40 && params.maxLines == SINGLE_LINE) {
            isBoring = layoutBuildHelper.getBoringMetrics(text, params.paint)
        }
        val precomputedTextWidth = when {
            isBoring != null -> {
                isBoring.width
                    .coerceAtMost(limitedTextWidth)
                    .coerceAtLeast(minTextWidth)
            }
            !isWrappedWidth -> {
                val (width, lastIndex) = params.paint.getTextWidth(
                    text = text,
                    maxWidth = limitedTextWidth,
                    byLayout = hasTextSizeSpans
                )
                lineLastSymbolIndex = lastIndex
                width.coerceAtLeast(minTextWidth)
            }
            else -> {
                val width = params.paint.getTextWidth(text = text, byLayout = hasTextSizeSpans)
                width.coerceAtMost(limitedTextWidth)
                    .coerceAtLeast(minTextWidth)
            }
        }

        return TextLayoutPrecomputedData(
            precomputedTextWidth = precomputedTextWidth,
            boring = isBoring,
            lineLastSymbolIndex = lineLastSymbolIndex,
            hasTextSizeSpans = hasTextSizeSpans
        )
    }
}

private const val SINGLE_LINE = 1
private val stateLazyMode = LazyThreadSafetyMode.NONE