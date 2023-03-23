package com.example.simpletextview.custom_tools.text_layout.core.helpers

import android.text.BoringLayout
import android.text.Layout
import android.text.TextPaint
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutPrecomputedData
import com.example.simpletextview.custom_tools.utils.LayoutBuilder

internal class TextLayoutBuildHelper {

    /**
     * Последний индекс символа в первой строке.
     * Используется для точечной оптимизации создания многострочного [Layout] с длинным текстом.
     */
    var precomputedData: TextLayoutPrecomputedData? = null
        set(value) {
            field = value
            if (value?.boring != null) {
                boring = value.boring
            }
        }

    private var boring: BoringLayout.Metrics? = null
    private var boringLayout: BoringLayout? = null

    fun getBoringMetrics(text: CharSequence, paint: TextPaint): BoringLayout.Metrics? =
        BoringLayout.isBoring(text, paint, boring)

    /**
     * Обновить разметку по набору параметров [params].
     * Если ширина в [params] не задана, то будет использована ширина текста.
     * Созданная разметка помещается в кэш [cachedLayout].
     */
    fun buildLayout(
        text: CharSequence,
        width: Int,
        maxHeight: Int?,
        fadingEdge: Boolean,
        params: TextLayoutParams
    ): Layout {
        val layout = LayoutBuilder(
            text = text,
            boring = boring,
            boringLayout = boringLayout,
            width = precomputedData?.precomputedTextWidth ?: width,
            maxHeight = maxHeight,
            paint = params.paint,
            alignment = params.alignment,
            ellipsize = params.ellipsize,
            includeFontPad = params.includeFontPad,
            spacingAdd = params.spacingAdd,
            spacingMulti = params.spacingMulti,
            maxLines = params.maxLines,
            highlights = params.highlights,
            breakStrategy = params.breakStrategy,
            hyphenationFrequency = params.hyphenationFrequency,
            fadingEdge = fadingEdge,
            calculatedLineLastIndex = precomputedData?.calculatedLineLastIndex,
            containsAbsoluteSizeSpans = precomputedData?.containsAbsoluteSizeSpans
        ).build()
        precomputedData = null
        return layout
    }
}