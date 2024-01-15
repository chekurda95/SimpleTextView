package com.example.simpletextview.custom_tools.utils.layout

import android.text.*

/**
 * Фабрика по созданию одной из реализаций [Layout] для отрисовки текста.
 *
 * @author am.boldinov
 */
interface LayoutFactory {

    /**
     * Создаёт новый объект [Layout].
     */
    fun create(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        textLength: Int = text.length,
        spacingMulti: Float = DEFAULT_SPACING_MULTI,
        spacingAdd: Float = DEFAULT_SPACING_ADD,
        includeFontPad: Boolean = false,
        maxLines: Int = SINGLE_LINE,
        isSingleLine: Boolean = false,
        breakStrategy: Int = 0,
        hyphenationFrequency: Int = 0,
        ellipsize: TextUtils.TruncateAt? = null,
        textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR,
        boring: BoringLayout.Metrics? = null,
        boringLayout: BoringLayout? = null,
        leftIndents: IntArray? = null,
        rightIndents: IntArray? = null
    ): Layout

    companion object {
        const val DEFAULT_SPACING_ADD = 0f
        const val DEFAULT_SPACING_MULTI = 1f
        const val SINGLE_LINE = 1
        internal const val RTL_SYMBOLS_CHECK_COUNT_LIMIT = 10
    }
}