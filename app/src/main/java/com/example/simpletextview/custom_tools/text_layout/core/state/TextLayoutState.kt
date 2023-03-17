package com.example.simpletextview.custom_tools.text_layout.core.state

import androidx.annotation.FloatRange
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams

internal class TextLayoutState(
    val params: TextLayoutParams,
    val drawParams: TextLayoutDrawParams
) {

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


}