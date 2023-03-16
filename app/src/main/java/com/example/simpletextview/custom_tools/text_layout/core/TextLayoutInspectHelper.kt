package com.example.simpletextview.custom_tools.text_layout.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import com.example.simpletextview.custom_tools.text_layout.TextLayout

/**
 * Вспомогательный класс для отладки текстовой разметки.
 * Позволяет отображать границы [TextLayout], а также внутренние отступы.
 * Может оказаться крайне полезным на этапе интеграции [TextLayout].
 */
internal class TextLayoutInspectHelper {

    /**
     * Краска линии границы по периметру [TextLayout].
     */
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
    }

    /**
     * Краска внутренних отступов [TextLayout].
     */
    val paddingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    val borderPath = Path()
    val borderRectF = RectF()
    val paddingPath = Path()
    val textBackgroundPath = Path()

    /**
     * Обновить информацию о разметке.
     */
    fun updateInfo(
        layout: Layout,
        rect: Rect,
        textPos: Pair<Float, Float>
    ) {
        borderPath.reset()
        textBackgroundPath.reset()
        paddingPath.reset()

        borderRectF.set(
            rect.left.toFloat() + ONE_PX,
            rect.top.toFloat() + ONE_PX,
            rect.right.toFloat() - ONE_PX,
            rect.bottom.toFloat() - ONE_PX
        )
        borderPath.addRect(borderRectF, Path.Direction.CW)

        textBackgroundPath.addRect(
            textPos.first,
            textPos.second,
            textPos.first + layout.width,
            textPos.second + layout.height,
            Path.Direction.CW
        )
        paddingPath.addRect(borderRectF, Path.Direction.CW)
        paddingPath.op(textBackgroundPath, Path.Op.DIFFERENCE)
    }

    /**
     * Нарисовать отладочные границы разметки.
     */
    fun draw(canvas: Canvas, isVisible: Boolean) {
        if (isVisible) {
            canvas.drawPath(paddingPath, paddingPaint)
            canvas.drawPath(borderPath, borderPaint)
        }
    }
}

private const val ONE_PX = 1