/**
 * Утилиты для графических красок android.
 *
 * @author vv.chekurda
 */
package com.example.simpletextview.custom_tools.utils

import android.graphics.Paint
import android.text.TextPaint
import androidx.annotation.Px

/**
 * Обычный anti-alias [Paint] с возможностью настройки прямо в конструкторе.
 * Несколько упрощает синтаксис создания обычного Paint.
 */
class SimplePaint(config: (SimplePaint.() -> Unit)? = null) : Paint(ANTI_ALIAS_FLAG) {
    init {
        config?.invoke(this)
    }
}

/**
 * Обычный anti-alias [TextPaint] с возможностью настройки прямо в конструкторе.
 * Несколько упрощает синтаксис создания обычного TextPaint.
 */
class SimpleTextPaint(config: (SimpleTextPaint.() -> Unit)? = null) : TextPaint(ANTI_ALIAS_FLAG) {

    /**
     * Установить максимальный размер текста в пикселях.
     * Использовать для явного ограничения размера при использовании значений sp.
     */
    @Px
    var maxTextSize: Int = Int.MAX_VALUE
        set(value) {
            field = value.coerceAtLeast(0)
            textSize = textSize.coerceAtMost(field.toFloat())
        }

    /**
     * Установить минимальный размер текста в пикселях.
     * Использовать для явного ограничения размера при использовании значений sp.
     */
    @Px
    var minTextSize: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            textSize = textSize.coerceAtLeast(field.toFloat())
        }

    init {
        config?.invoke(this)
    }

    override fun setTextSize(textSize: Float) {
        val limitedTextSize = textSize
            .coerceAtLeast(minTextSize.toFloat())
            .coerceAtMost(maxTextSize.toFloat())
        super.setTextSize(limitedTextSize)
    }
}

const val PAINT_MAX_ALPHA = 255