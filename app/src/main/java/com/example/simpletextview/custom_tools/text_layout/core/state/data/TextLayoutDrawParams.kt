package com.example.simpletextview.custom_tools.text_layout.core.state.data

import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import androidx.annotation.FloatRange
import com.example.simpletextview.custom_tools.text_layout.TextLayout
import com.example.simpletextview.custom_tools.utils.PAINT_MAX_ALPHA

internal class TextLayoutDrawParams {

    var drawingLayout: Layout? = null

    /**
     * Координаты границ [TextLayout], полученные в [TextLayout.layout].
     */
    val layoutRect = Rect()

    val textRect = RectF()

    /**
     * Прозрачность текста разметки.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    var layoutAlpha: Float = 1f

    /**
     * Прозрачность цвета краски текста.
     */
    var textColorAlpha = PAINT_MAX_ALPHA

    /**
     * Поворот текста вокруг центра на угол в градусах.
     */
    var rotation = 0f

    /**
     * Смещение отрисовки разметки по оси X.
     */
    var translationX: Float = 0f

    /**
     * Смещение отрисовки разметки по оси Y.
     */
    var translationY: Float = 0f
}