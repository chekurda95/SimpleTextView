package com.example.simpletextview.simple_tv

import android.graphics.Rect
import android.text.method.TransformationMethod
import android.view.View

internal class AllCapsTransformationMethod : TransformationMethod {

    override fun getTransformation(source: CharSequence, view: View): CharSequence =
        source.toString().uppercase()

    override fun onFocusChanged(
        view: View, sourceText: CharSequence, focused: Boolean,
        direction: Int, previouslyFocusedRect: Rect,
    ) = Unit
}