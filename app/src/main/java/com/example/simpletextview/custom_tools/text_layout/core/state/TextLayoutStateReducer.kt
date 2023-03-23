package com.example.simpletextview.custom_tools.text_layout.core.state

import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfig
import com.example.simpletextview.custom_tools.text_layout.core.TextLayoutConfiguratorImpl
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutBuildHelper
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutDrawableStateHelper
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParamsDiff

internal class TextLayoutStateReducer(
    private val layoutBuildHelper: TextLayoutBuildHelper,
    private val drawableStateHelper: TextLayoutDrawableStateHelper
) {

    fun reduceInitialState(initialParams: TextLayoutParams, config: TextLayoutConfig?): TextLayoutState {
        val initialDrawParams = TextLayoutDrawParams()
        val newState = if (config != null) {
            val (params, _) = TextLayoutConfiguratorImpl(initialParams).apply(config).configure()
            initialDrawParams.textColorAlpha = params.paint.alpha
            TextLayoutState(layoutBuildHelper, params, initialDrawParams)
        } else {
            TextLayoutState(layoutBuildHelper, initialParams, initialDrawParams)
        }
        drawableStateHelper.textPaint = newState.params.paint
        return newState
    }

    inline operator fun invoke(state: TextLayoutState, config: TextLayoutConfig): Pair<TextLayoutState, Boolean> {
        var isStateChanged = false
        val (params, diff) = TextLayoutConfiguratorImpl(state.params).apply(config).configure()

        if (diff.isPaintAlphaChanged) {
            state.drawParams.textColorAlpha = params.paint.alpha
            params.paint.alpha = (state.drawParams.textColorAlpha * state.drawParams.layoutAlpha).toInt()
        }

        val newState = if (diff.isLayoutChanged()) {
            isStateChanged = true
            TextLayoutState(layoutBuildHelper, params, state.drawParams)
        } else {
            state
        }
        drawableStateHelper.textPaint = newState.params.paint

        return newState to isStateChanged
    }

    private fun TextLayoutParamsDiff.isLayoutChanged(): Boolean =
        isTextParamsChanged || isPaddingParamsChanged || isVisibilityParamsChanged
}