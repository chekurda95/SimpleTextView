package com.example.simpletextview.custom_tools.text_layout.core.helpers

import com.example.simpletextview.custom_tools.text_layout.core.state.TextLayoutState
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams

internal class TextLayoutStateBuilder(
    private val layoutBuildHelper: TextLayoutBuildHelper,
    private val fadingEdgeHelper: TextLayoutFadingEdgeHelper,
    private val drawableStateHelper: TextLayoutDrawableStateHelper
) {

    fun createState(
        params: TextLayoutParams,
        drawParams: TextLayoutDrawParams
    ) : TextLayoutState =
        TextLayoutState(
            layoutBuildHelper = layoutBuildHelper,
            fadingEdgeHelper = fadingEdgeHelper,
            params = params,
            drawParams = drawParams
        ).also {
            drawableStateHelper.textPaint = it.params.paint
        }
}