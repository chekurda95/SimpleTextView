package com.example.simpletextview.custom_tools.text_layout.core.helpers

import android.text.BoringLayout
import android.text.Layout
import android.text.TextPaint
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutPrecomputedData
import com.example.simpletextview.custom_tools.utils.layout.LayoutConfigurator

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
        val layout = LayoutConfigurator.createLayout {
            this.text = text
            this.boring = this@TextLayoutBuildHelper.boring
            this.boringLayout = this@TextLayoutBuildHelper.boringLayout
            this.width = precomputedData?.precomputedTextWidth ?: width
            this.paint = params.paint
            this.maxHeight = maxHeight
            this.alignment = params.alignment
            this.ellipsize = params.ellipsize
            this.includeFontPad = params.includeFontPad
            this.spacingAdd = params.spacingAdd
            this.spacingMulti = params.spacingMulti
            this.maxLines = params.maxLines
            this.highlights = params.highlights
            this.breakStrategy = params.breakStrategy
            this.hyphenationFrequency = params.hyphenationFrequency
            this.fadingEdge = fadingEdge
            this.lineLastSymbolIndex = precomputedData?.lineLastSymbolIndex
            this.hasTextSizeSpans = precomputedData?.hasTextSizeSpans
        }
        precomputedData = null
        return layout
    }
}