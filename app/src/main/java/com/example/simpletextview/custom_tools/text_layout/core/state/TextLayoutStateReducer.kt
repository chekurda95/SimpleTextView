package com.example.simpletextview.custom_tools.text_layout.core.state

import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfig
import com.example.simpletextview.custom_tools.text_layout.core.TextLayoutDiffHandler
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutStateBuilder
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParamsDiff

internal class TextLayoutStateReducer(private val stateBuilder: TextLayoutStateBuilder) {

    fun reduceInitialState(initialParams: TextLayoutParams, config: TextLayoutConfig?): TextLayoutState {
        val drawParams = TextLayoutDrawParams()
        val params = if (config != null) {
            val (newParams, _) = TextLayoutDiffHandler(initialParams).perform(config)
            drawParams.textColorAlpha = newParams.paint.alpha
            newParams
        } else {
            initialParams
        }
        return stateBuilder.createState(params, drawParams)
    }

    inline operator fun invoke(state: TextLayoutState, config: TextLayoutConfig): Pair<TextLayoutState, Boolean> {
        var isStateChanged = false
        val (newParams, diff) = TextLayoutDiffHandler(state.params).perform(config)
        val drawParams = state.drawParams

        if (diff.isPaintAlphaChanged) {
            state.drawParams.textColorAlpha = newParams.paint.alpha
            newParams.paint.alpha = (state.drawParams.textColorAlpha * state.drawParams.layoutAlpha).toInt()
        }

        val newState = if (diff.isLayoutChanged()) {
            isStateChanged = true
            stateBuilder.createState(newParams, drawParams)
        } else {
            state
        }

        return newState to isStateChanged
    }

    private fun TextLayoutParamsDiff.isLayoutChanged(): Boolean =
        isTextParamsChanged || isPaddingParamsChanged || isVisibilityParamsChanged
}