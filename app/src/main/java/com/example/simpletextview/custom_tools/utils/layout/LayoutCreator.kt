package com.example.simpletextview.custom_tools.utils.layout

import android.os.Build
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import java.lang.reflect.Constructor

/**
 * Утилита для создания текстовой разметки [Layout] на базе [StaticLayout] или [BoringLayout].
 * Является удобной оберткой над конструкторами/билдерами с дефолтными значениями.
 *
 * Не включает в себя никакого дополнительного функционала или оптимизаций.
 * @see LayoutConfigurator
 *
 * @author vv.chekurda
 */
object LayoutCreator {

    private val hiddenStaticConstructor: Constructor<StaticLayout> by lazy(LazyThreadSafetyMode.NONE) {
        StaticLayout::class.java.getConstructor(
            CharSequence::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            TextPaint::class.java,
            Int::class.javaPrimitiveType,
            Layout.Alignment::class.java,
            TextDirectionHeuristic::class.java,
            Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            TextUtils.TruncateAt::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ).apply {
            isAccessible = true
        }
    }

    /**
     * Создать текстовую разметку [Layout].
     *
     * @property text текст, который будет находиться в [StaticLayout].
     * @property paint краска, которой будет рисоваться текст [text].
     * @property width ширина контейнера текста. По-умолчанию ширина текста [text].
     * @property alignment мод выравнивания текста. По-умолчанию выравнивание по левому краю.
     * @property ellipsize мод сокращения текста. По-умолчанию текст сокращается в конце.
     * @property includeFontPad true, если необходимо учитывать отступы шрифта, аналог атрибута includeFontPadding.
     * @property spacingAdd величина межстрочного интервала.
     * @property spacingMulti множитель межстрочного интервала.
     * @property maxLines максимально допустимое количество строк, аналогично механике [TextView.setMaxLines].
     * Null - без ограничений.
     * @property isSingleLine true, если при использовании [boring] необходимо создать именно [BoringLayout].
     * Логика кажется странной, но у [TextView] на singleLine безусловно создается [BoringLayout],
     * а с maxLines == 1 только в случае, когда ширина текста меньше [width].
     * @property breakStrategy стратегия разрыва строки, см [Layout.BREAK_STRATEGY_SIMPLE].
     * @property hyphenationFrequency частота переноса строк, см. [Layout.HYPHENATION_FREQUENCY_NONE].
     * @property textDir направление текста.
     */
    fun createLayout(
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
        boringLayout: BoringLayout? = null
    ): Layout {
        val checkedBoring = checkBoring(boring, width, maxLines, isSingleLine, ellipsize)
        val includeFontPadding = if (checkedBoring != null ||
            !textDir.isRtl(text, 0, textLength.coerceAtMost(RTL_SYMBOLS_CHECK_COUNT_LIMIT))
        ) {
            includeFontPad
        } else {
            true
        }
        return if (checkedBoring != null) {
            createBoringLayout(
                boring = checkedBoring,
                text = text,
                paint = paint,
                width = width,
                alignment = alignment,
                spacingMulti = spacingMulti,
                spacingAdd = spacingAdd,
                includeFontPad = includeFontPadding,
                ellipsize = ellipsize,
                boringLayout = boringLayout
            )
        } else {
            createStaticLayout(
                text = text,
                paint = paint,
                width = width,
                alignment = alignment,
                textLength = textLength,
                spacingMulti = spacingMulti,
                spacingAdd = spacingAdd,
                includeFontPad = includeFontPadding,
                maxLines = maxLines,
                breakStrategy = breakStrategy,
                hyphenationFrequency = hyphenationFrequency,
                ellipsize = ellipsize,
                textDir = textDir
            )
        }
    }

    private fun createBoringLayout(
        boring: BoringLayout.Metrics,
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        spacingMulti: Float = DEFAULT_SPACING_MULTI,
        spacingAdd: Float = DEFAULT_SPACING_ADD,
        includeFontPad: Boolean = false,
        ellipsize: TextUtils.TruncateAt? = null,
        boringLayout: BoringLayout? = null
    ): BoringLayout {
        val boringEllipsize = ellipsize?.takeIf { boring.width > width }
        return if (boringLayout != null) {
            boringLayout.replaceOrMake(
                text,
                paint,
                width,
                alignment,
                spacingMulti,
                spacingAdd,
                boring,
                includeFontPad,
                boringEllipsize,
                width
            )
        } else {
            BoringLayout.make(
                text,
                paint,
                width,
                alignment,
                spacingMulti,
                spacingAdd,
                boring,
                includeFontPad,
                boringEllipsize,
                width
            )
        }
    }

    private fun createStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        textLength: Int = text.length,
        spacingMulti: Float = DEFAULT_SPACING_MULTI,
        spacingAdd: Float = DEFAULT_SPACING_ADD,
        includeFontPad: Boolean = false,
        maxLines: Int = 1,
        breakStrategy: Int = 0,
        hyphenationFrequency: Int = 0,
        ellipsize: TextUtils.TruncateAt? = null,
        textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR
    ): StaticLayout =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, textLength, paint, width)
                .setAlignment(alignment)
                .setLineSpacing(spacingAdd, spacingMulti)
                .setIncludePad(includeFontPad)
                .setMaxLines(maxLines)
                .setBreakStrategy(breakStrategy)
                .setHyphenationFrequency(hyphenationFrequency).apply {
                    if (ellipsize != null) {
                        setEllipsize(ellipsize)
                        setEllipsizedWidth(width)
                    }
                }
                .setTextDirection(textDir)
                .build()
        } else {
            hiddenStaticConstructor.newInstance(
                text,
                0,
                textLength,
                paint,
                width,
                alignment,
                textDir,
                spacingMulti,
                spacingAdd,
                includeFontPad,
                ellipsize,
                width,
                maxLines
            )
        }

    private fun checkBoring(
        boring: BoringLayout.Metrics?,
        width: Int,
        maxLines: Int,
        isSingleLine: Boolean,
        ellipsize: TextUtils.TruncateAt?
    ): BoringLayout.Metrics? =
        boring?.takeIf {
            isSingleLine || boring.width <= width || (ellipsize != null && maxLines == SINGLE_LINE)
        }
}

private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f
private const val SINGLE_LINE = 1
internal const val RTL_SYMBOLS_CHECK_COUNT_LIMIT = 10