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
 *
 * @param text текст, для которого высчитывается ширина.
 * @param start индекс символа, с которого начнется измерение. (включительно)
 * @param end индекс символа, на котором закончится измерение. (не включительно, +1 к индексу символа)
 * @param byLayout измерение по api [Layout]. Тяжелее, но позволяет получить реальную ширину для spannable текста
 * со спанами размера.
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

/**
 * Получить ширину текста и индекс последнего символа для данного [TextPaint] с ограничением в [maxWidth].
 * Метод позволяет не производить лишних измерений текста, если он не влезает в ограничение [maxWidth].
 *
 * @param text текст, для которого высчитывается ширина.
 * @param maxWidth максимально допустимое пространство для текста.
 * @param byLayout измерение по api [Layout]. Тяжелее, но позволяет получить реальную ширину для spannable текста
 * со спанами размера.
 *
 * @return пара - ширина текста, но не больше [maxWidth], и индекс последнего символа во время ручного измерения.
 * Важно: индекс последнего символа может находиться за пределами [maxWidth], его необходимо использовать
 * исключительно для построения статичной текстовой разметки для оптимизации измерений, чтобы не гонять
 * layout по тексту за пределами видимости.
 * Пример: текст на 200 символов, в layout максимум может отобразиться 50, передача индекса в 50
 * при построении разметки почти в 3 раза ускоряет построение за счет исключения обработки лишнего текста.
 */
fun TextPaint.getTextWidth(
    text: CharSequence,
    maxWidth: Int,
    byLayout: Boolean = false,
    checkMultiLines: Boolean = true
): Pair<Int, Int> {
    if (maxWidth <= 0) return 0 to 0
    val (correctText, length) = text.correctTextAndLength(checkMultiLines)
    return if (length > MANUAL_TEXT_MEASURE_LENGTH && !byLayout) {
        val step = MANUAL_TEXT_MEASURE_SYMBOLS_STEP
        val steps = ceil(length / step.toFloat()).toInt()
        var sumWidth = 0f
        var startIndex = 0
        var lastIndex = 0

        for (i in 1..steps) {
            lastIndex = (i * step).coerceAtMost(length)
            sumWidth += getTextWidth(correctText, startIndex, lastIndex)
            if (sumWidth >= maxWidth) {
                return maxWidth to lastIndex
            } else {
                startIndex = lastIndex
            }
        }

        sumWidth.toInt() to lastIndex
    } else {
        val textWidth = this@getTextWidth.getTextWidth(correctText, byLayout = byLayout).coerceAtMost(maxWidth)
        textWidth to length
    }
}

private fun CharSequence.correctTextAndLength(checkMultiLines: Boolean): Pair<CharSequence, Int> {
    var longestLength = 0
    var longestString: CharSequence = ""
    if (checkMultiLines && this.contains("\n")) {
        val strings = this.split("\n")
        strings.forEach {
            if (it.length > longestLength) {
                longestLength = it.length
                longestString = it
            }
        }
    } else {
        longestLength = this.length
        longestString = this
    }
    return longestString to longestLength
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

private const val MANUAL_TEXT_MEASURE_LENGTH = 20
private const val MANUAL_TEXT_MEASURE_SYMBOLS_STEP = 10