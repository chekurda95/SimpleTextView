/**
 * Набор инструментов для облегчения работы с кастомными view.
 *
 * @author vv.chekurda
 */
package com.example.simpletextview.custom_tools.utils

import android.content.res.Resources
import android.text.Layout
import android.text.TextPaint
import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.Px
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Безопасный вызов [View.requestLayout].
 * Включает проверку на уже выполненный ранее запрос [View.requestLayout],
 * чтобы не гонять лишние строки кода + включает в себя [View.invalidate],
 * тк вызов [View.requestLayout] не гарантирует вызов [View.draw] в рамках жизненного цикла [View].
 *
 * Проверка [View.isInLayout] намеренно не используется
 * https://online.sbis.ru/opendoc.html?guid=459be774-844c-4235-96e1-be8284725a15
 */
fun View.safeRequestLayout() {
    if (!isLayoutRequested) requestLayout()
    invalidate()
}

/**
 * Разместить View на координате [x] [y] (левый верхний угол View) с рассчитанными размерами в [View.onMeasure].
 */
fun View.layout(x: Int, y: Int) {
    layout(x, y, x + measuredWidth, y + measuredHeight)
}

/**
 * Получить значение в пикселях по переданному значению в dp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun Resources.dp(@FloatRange(from = 0.0) value: Float): Int =
    (displayMetrics.density * value).mathRoundToInt()

/**
 * Получить значение в пикселях по переданному значению в dp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun Resources.dp(@IntRange(from = 0) value: Int): Int =
    dp(value.toFloat())

/**
 * Получить значение в пикселях по переданному значению в sp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun Resources.sp(@FloatRange(from = 0.0) value: Float): Int =
    (displayMetrics.scaledDensity * value).mathRoundToInt()

/**
 * Получить значение в пикселях по переданному значению в sp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun Resources.sp(@IntRange(from = 0) value: Int): Int =
    sp(value.toFloat())

/**
 * Получить значение в пикселях по переданному значению в dp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun View.dp(@FloatRange(from = 0.0) value: Float): Int =
    resources.dp(value)

/**
 * Получить значение в пикселях по переданному значению в dp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun View.dp(@IntRange(from = 0) value: Int): Int =
    dp(value.toFloat())

/**
 * Получить значение в пикселях по переданному значению в sp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun View.sp(@FloatRange(from = 0.0) value: Float): Int =
    resources.sp(value)

/**
 * Получить значение в пикселях по переданному значению в sp,
 * округленное до целого числа (по правилам округления).
 */
@Px
fun View.sp(@IntRange(from = 0) value: Int): Int =
    sp(value.toFloat())

/**
 * Получить ширину текста для данного [TextPaint].
 */
@Px
fun TextPaint.getTextWidth(
    text: CharSequence,
    start: Int = 0,
    end: Int = text.length,
    byLayout: Boolean = false
): Int =
    if (byLayout) {
        ceil(Layout.getDesiredWidth(text, start, end, this)).toInt()
    } else {
        measureText(text, start, end).toInt()
    }

fun TextPaint.getTextWidth(text: CharSequence, maxWidth: Int, byLayout: Boolean = false): Pair<Int, Int> {
    if (maxWidth <= 0) return 0 to 0

    val length = text.length
    return if (length > 20) {
        val step = 10
        val steps = ceil(length / step.toFloat()).toInt()
        var sumWidth = 0f
        var startIndex = 0
        var lastIndex = 0
        for (i in 1..steps) {
            lastIndex = (i * step).coerceAtMost(length)
            sumWidth += getTextWidth(text, startIndex, lastIndex, byLayout)
            if (sumWidth >= maxWidth) {
                return maxWidth to lastIndex
            } else {
                startIndex = lastIndex
            }
        }
        sumWidth.toInt() to lastIndex
    } else {
        val textWidth = this@getTextWidth.getTextWidth(text, byLayout = byLayout).coerceAtMost(maxWidth)
        textWidth to length
    }
}

/**
 * Получить высоту одной строчки текста для данного [TextPaint].
 */
@get:Px
val TextPaint.textHeight: Int
    get() = ceil(fontMetrics.descent - fontMetrics.ascent).toInt()

/**
 * Метод для правильного математического округления дробных чисел по модулю.
 * [Math.round] округляет отрицательные половинчатые числа к бОльшему значению, а не по модулю
 * (например, round(-1.5) == -1 и round(1.5) == 2),
 * и логика этого метода разнится с округлением значений из ресурсов.
 * http://proglang.su/java/numbers-round
 */
internal fun Float.mathRoundToInt(): Int =
    abs(this).roundToInt().let { result ->
        if (this >= 0) result
        else result * -1
    }