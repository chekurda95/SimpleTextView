package com.example.simpletextview.custom_tools.text_layout.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.text.TextUtils
import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutParams

internal class TextLayoutFadingEdgeHelper {

    private val fadeMatrix by lazy { Matrix() }

    private val fadePaint by lazy {
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
    }

    private var fadeShader: Lazy<Shader>? = null

    /**
     * Признак необходимости затенения каря текста, когда он не помещается в рзметку
     */
    var isFadeEdgeVisible: Boolean = false

    /**
     * Признак необходимости показа затемнения текста при сокращении.
     */
    var requiresFadingEdge: Boolean = false

    /**
     * Ширина затенения текста, если он не помещается в разметку.
     */
    var fadeEdgeSize: Int = 0
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) fadeShader = createFadeShader()
        }

    /**
     * Обновить признак затенения каря для слишком длинного текста [isFadeEdgeVisible].
     */
    fun updateFadeEdgeVisibility(params: TextLayoutParams) {
        isFadeEdgeVisible = requiresFadingEdge && fadeEdgeSize > 0
                && params.maxLines == 1
                && params.text != TextUtils.ellipsize(
            params.text,
            params.paint,
            params.textWidth.toFloat(),
            TextUtils.TruncateAt.END
        )
    }

    fun drawFade(canvas: Canvas, rect: Rect, function: (Canvas) -> Unit) {
        val saveCount = canvas.saveLayer(0f, 0f, rect.right.toFloat(), rect.bottom.toFloat(), null)
        val fadeLeft = rect.right.toFloat() - fadeEdgeSize
        function(canvas)
        fadeMatrix.reset()
        fadeMatrix.postTranslate(fadeLeft, 0f)
        fadeShader?.value?.setLocalMatrix(fadeMatrix)
        canvas.drawRect(
            fadeLeft,
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            fadePaint
        )
        canvas.restoreToCount(saveCount)
    }

    private fun createFadeShader(): Lazy<Shader> = lazy {
        LinearGradient(
            0f,
            0f,
            fadeEdgeSize.toFloat(),
            0f,
            Color.TRANSPARENT,
            Color.WHITE,
            Shader.TileMode.CLAMP
        ).also {
            fadePaint.shader = it
        }
    }
}