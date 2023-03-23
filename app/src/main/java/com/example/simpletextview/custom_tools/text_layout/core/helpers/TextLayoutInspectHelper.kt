package com.example.simpletextview.custom_tools.text_layout.core.helpers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.toRectF
import com.example.simpletextview.custom_tools.text_layout.TextLayout
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams

/**
 * Вспомогательный класс для отладки текстовой разметки.
 * Позволяет отображать границы [TextLayout], а также внутренние отступы.
 * Может оказаться крайне полезным на этапе интеграции [TextLayout].
 */
internal class TextLayoutInspectHelper {

    /**
     * Краска линии границы по периметру [TextLayout].
     */
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
    }

    /**
     * Краска внутренних отступов [TextLayout].
     */
    private val paddingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    private val borderPath = Path()
    private val borderRectF = RectF()
    private val paddingPath = Path()
    private val textBackgroundPath = Path()

    /**
     * Обновить информацию о разметке.
     */
    fun updateInfo(drawParams: TextLayoutDrawParams) {
        borderPath.reset()
        textBackgroundPath.reset()
        paddingPath.reset()

        val rect = drawParams.layoutRect
        borderRectF.set(
            rect.left.toFloat() + ONE_PX,
            rect.top.toFloat() + ONE_PX,
            rect.right.toFloat() - ONE_PX,
            rect.bottom.toFloat() - ONE_PX
        )
        borderPath.addRect(borderRectF, Path.Direction.CW)
        textBackgroundPath.addRect(drawParams.textRect, Path.Direction.CW)
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