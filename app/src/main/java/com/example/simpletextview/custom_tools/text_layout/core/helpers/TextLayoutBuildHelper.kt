package com.example.simpletextview.custom_tools.text_layout.core.helpers

import android.text.BoringLayout
import android.text.Layout
import android.text.Spannable
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.utils.LayoutBuilder

internal class TextLayoutBuildHelper(
    private val fadingEdgeHelper: TextLayoutFadingEdgeHelper
) {

    private var boring: BoringLayout.Metrics? = null
    private var boringLayout: BoringLayout? = null
    private val useFadingEdge: Boolean
        get() = fadingEdgeHelper.requiresFadingEdge && fadingEdgeHelper.fadeEdgeSize > 0

    /**
     * Обновить разметку по набору параметров [params].
     * Если ширина в [params] не задана, то будет использована ширина текста.
     * Созданная разметка помещается в кэш [cachedLayout].
     */
    fun buildLayout(
        text: CharSequence,
        width: Int,
        maxHeight: Int?,
        params: TextLayoutParams
    ): Layout {
        if (text !is Spannable) {
            boring = BoringLayout.isBoring(text, params.paint, boring)
        }
        return LayoutBuilder(
            text = text,
            boring = boring,
            boringLayout = boringLayout,
            width = width,
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
            fadingEdge = useFadingEdge
        ).build().also {
            if (it is BoringLayout) boringLayout = it
            fadingEdgeHelper.updateFadeEdgeVisibility(width, params)
        }
    }
}