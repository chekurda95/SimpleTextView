package com.example.simpletextview.custom_tools.text_layout.core.state

import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfig
import com.example.simpletextview.custom_tools.text_layout.core.TextLayoutConfiguratorImpl
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParamsDiff

internal class TextLayoutStateReducer {

    fun reduceInitialState(initialParams: TextLayoutParams, config: TextLayoutConfig?): TextLayoutState {
        val initialDrawParams = TextLayoutDrawParams()
        return if (config != null) {
            val (params, _) = TextLayoutConfiguratorImpl(initialParams).apply(config).configure()
            initialDrawParams.textPos = params.padding.start.toFloat() to params.padding.top.toFloat()
            initialDrawParams.textColorAlpha = params.paint.alpha
            TextLayoutState(params, initialDrawParams)
        } else {
            TextLayoutState(initialParams, initialDrawParams)
        }
    }

    inline operator fun invoke(state: TextLayoutState, config: TextLayoutConfig): TextLayoutState {
        val (params, diff) = TextLayoutConfiguratorImpl(state.params).apply(config).configure()

        if (diff.isPaintAlphaChanged) {
            state.drawParams.textColorAlpha = params.paint.alpha
            params.paint.alpha = (state.drawParams.textColorAlpha * state.drawParams.layoutAlpha).toInt()
        }

        return if (diff.isLayoutChanged()) {
            TextLayoutState(params, state.drawParams)
        } else {
            state
        }
    }

    private fun TextLayoutParamsDiff.isLayoutChanged(): Boolean =
        isTextParamsChanged || isPaddingParamsChanged || isVisibilityParamsChanged
}