package com.example.simpletextview.custom_tools.utils

import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.text.*
import android.text.Layout.Alignment
import android.text.StaticLayout.BREAK_STRATEGY_HIGH_QUALITY
import android.text.TextUtils.TruncateAt
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.utils.StaticLayoutConfigurator.Companion.createStaticLayout
import kotlin.math.ceil

/**
 * Конфигуратор для создания статичной текстовой разметки [StaticLayout].
 * @see createStaticLayout
 *
 * Является модернизированной оберткой над [StaticLayout.Builder] и служит для упрощения создания
 * однострочной и многострочной текстовой разметки [StaticLayout], скрывая алгоритмы подбора аргументов.
 * Также имеет расширенные возможности, например, выделение текста через [highlights] для сценариев поиска в реестрах.
 *
 * @property text текст, который будет находиться в [StaticLayout].
 * @property paint краска, которой будет рисоваться текст [text].
 * @param config конфигурация для изменения нижеупомянутых параметров.
 *
 * Настраиваемые параметры:
 * @property width ширина контейнера текста. По-умолчанию ширина текста [text].
 * @property alignment мод выравнивания текста. По-умолчанию выравнивание по левому краю.
 * @property ellipsize мод сокращения текста. По-умолчанию текст сокращается в конце.
 * @property includeFontPad true, если необходимо учитывать отступы шрифта, аналог атрибута includeFontPadding.
 * @property spacingAdd величина межстрочного интервала.
 * @property spacingMulti множитель межстрочного интервала.
 * @property maxLines максимально допустимое количество строк, аналогично механике [TextView.setMaxLines]. Null - без ограничений.
 * @property maxHeight максимально допустимая высота. Опционально необходима для ограничения количества строк высотой, а не значением.
 * @property highlights модель для выделения текста, например для сценариев поиска.
 * @property canContainUrl true, если строка может содержать url. Влияет на точность сокращения текста
 * @property breakStrategy стратегия разрыва строки, см [Layout.BREAK_STRATEGY_SIMPLE].
 * Если необходим только для ссылок, то лучше воспользоваться [canContainUrl].
 * @property hyphenationFrequency частота переноса строк, см. [Layout.HYPHENATION_FREQUENCY_NONE].
 * @property fadingEdge построение разметки с учетом возможного размыливания вместо сокращения текста.
 * и скорость создания [StaticLayout]. (Использовать только для [maxLines] > 1, когда текст может содержать ссылки)
 *
 * @author vv.chekurda
 */
class StaticLayoutConfigurator internal constructor(
    private var text: CharSequence,
    private var paint: TextPaint,
    @Px var width: Int = DEFAULT_WRAPPED_WIDTH,
    var alignment: Alignment = Alignment.ALIGN_NORMAL,
    var ellipsize: TruncateAt? = TruncateAt.END,
    var includeFontPad: Boolean = true,
    var spacingAdd: Float = DEFAULT_SPACING_ADD,
    var spacingMulti: Float = DEFAULT_SPACING_MULTI,
    @IntRange(from = 1) var maxLines: Int = SINGLE_LINE,
    @Px var maxHeight: Int? = null,
    var highlights: TextHighlights? = null,
    var canContainUrl: Boolean = false,
    var breakStrategy: Int = 0,
    var hyphenationFrequency: Int = 0,
    var fadingEdge: Boolean = false,
    private var config: StaticLayoutConfig? = null
) {
    companion object {

        /**
         * Создать статичную текстовую разметку [StaticLayout] с настройками [config].
         *
         * @param text текст, который будет находиться в [StaticLayout].
         * @param paint краска, которой будет рисоваться текст [text].
         */
        fun createStaticLayout(
            text: CharSequence,
            paint: TextPaint,
            config: StaticLayoutConfig? = null
        ): StaticLayout =
            StaticLayoutConfigurator(text, paint, config = config).configure()
    }

    /**
     * Применить настройки [config] для создания [StaticLayout].
     */
    internal fun configure(): StaticLayout {
        config?.invoke(this)
        configureMaxLines()
        configureWidth()
        configureText()
        return buildStaticLayout().let { layout ->
            if (layout.lineCount <= maxLines) {
                layout
            } else {
                val lastLineIndex = maxLines - 1
                val left = layout.getLineLeft(lastLineIndex)
                val lineWidth = layout.getLineWidth(lastLineIndex)
                val off = layout.getOffsetForHorizontal(lastLineIndex, if (left != 0f) left else lineWidth)
                val endIndex = maxOf(if (lineWidth < width) off else off - THREE_SYMBOLS_OFFSET_FROM_TELEGRAM, 0)
                text = SpannableStringBuilder(text.subSequence(0, endIndex)).append(HORIZONTAL_ELLIPSIS_CHAR)
                buildStaticLayout(canContainUrl)
            }
        }
    }

    /**
     * Настроить ширину текста.
     */
    private fun configureWidth() {
        width = when {
            text !is Spannable -> {
                maxOf(
                    width.takeIf { it != DEFAULT_WRAPPED_WIDTH }
                        ?: paint.getTextWidth(text),
                    0
                )
            }
            width == DEFAULT_WRAPPED_WIDTH || (maxLines == SINGLE_LINE && width == paint.getTextWidth(text)) -> {
                width = Int.MAX_VALUE
                val layout = buildStaticLayout()
                ceil(layout.getLineWidth(0)).toInt()
            }
            else -> {
                maxOf(0, width)
            }
        }
    }

    /**
     * Настроить максимально допустимое количество строк.
     */
    private fun configureMaxLines() {
        val calculatedMaxLines = when {
            text.isBlank() -> SINGLE_LINE
            maxHeight != null -> maxOf(maxHeight!!, 0) / getOneLineHeight()
            else -> maxLines
        }
        maxLines = maxOf(calculatedMaxLines, SINGLE_LINE)
    }

    /**
     * Настроить и вернуть текст.
     * При необходимости сокращает и подсвечивает текст по параметрам [highlights].
     */
    private fun configureText() {
        if (isNeedFade()) return
        text = when {
            // Текст без ограничений по высоте или ширине -> не пытаемся сокращать
            maxLines == MAX_LINES_NO_LIMIT -> {
                text.highlightText(highlights)
            }
            // Spannable текст с переносами с ограничением в maxLines -> парсим строки руками и при необходимости сокращаем
            maxLines > 0 && text.contains("\n") -> {
                if (text !is Spannable) {
                    text.split("\n")
                        .joinToString(separator = "\n", limit = maxLines)
                        .ellipsizeAndHighlightText(paint, width * maxLines, highlights)
                } else {
                    var lastHyphenationIndex = 0
                    repeat(maxLines) {
                        val index = text.indexOf("\n", lastHyphenationIndex + 1, false)
                        lastHyphenationIndex = index
                        if (index != -1) {
                            return@repeat
                        }
                    }
                    if (lastHyphenationIndex != -1) {
                        text.subSequence(0, lastHyphenationIndex)
                    } else {
                        text
                    }.ellipsizeAndHighlightText(paint, width * maxLines, highlights)
                }
            }
            // Многострочный текст с ограничением по ширине -> при необходимости сокращаем
            else -> {
                text.ellipsizeAndHighlightText(paint, width * maxLines, highlights)
            }
        }
    }

    /**
     * Построить [StaticLayout] по текущим параметрам конфигуратора.
     *
     * @param isBreakHighQuality true, если необходим качественный перенос строки
     * с оптимизацией переносов строк по всему абзацу.
     */
    @SuppressLint("WrongConstant", "Range")
    private fun buildStaticLayout(
        isBreakHighQuality: Boolean = false
    ): StaticLayout {
        val calculatedWidth = width + if (isNeedFade()) ADDITIONAL_WIDTH else 0
        return if (SDK_INT >= M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, calculatedWidth)
                .setAlignment(alignment)
                .setLineSpacing(spacingAdd, spacingMulti)
                .setIncludePad(includeFontPad)
                .setEllipsize(ellipsize)
                .setEllipsizedWidth(calculatedWidth)
                .setMaxLines(maxLines)
                .setBreakStrategy(if (isBreakHighQuality) BREAK_STRATEGY_HIGH_QUALITY else breakStrategy)
                .setHyphenationFrequency(hyphenationFrequency)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                0,
                text.length,
                paint,
                calculatedWidth,
                alignment,
                spacingMulti,
                spacingAdd,
                includeFontPad,
                ellipsize,
                calculatedWidth
            )
        }
    }

    private fun isNeedFade(): Boolean =
        fadingEdge && text != TextUtils.ellipsize(text, paint, width.toFloat(), TruncateAt.END)

    /**
     * Получить высоту одной строки текста по заданному [paint].
     */
    private fun getOneLineHeight(): Int =
        paint.textHeight.let { textHeight ->
            if (textHeight != 0 || paint.textSize == 0f) {
                textHeight
            } else {
                getOneLineHeightByStaticLayout()
            }
        }

    /**
     * Получить высоту одной строки текста путем создания [StaticLayout].
     *
     * Необходимость для тестов, где нет возможности замокать native [TextPaint],
     * который возвращает textSize == 0,
     * [StaticLayout] как-то обходит эту ситуацию через другие нативные методы.
     */
    private fun getOneLineHeightByStaticLayout(): Int {
        val paramsMaxLines = maxLines
        maxLines = 1
        return buildStaticLayout().height.also {
            maxLines = paramsMaxLines
        }
    }
}

private typealias StaticLayoutConfig = StaticLayoutConfigurator.() -> Unit

private const val MAX_LINES_NO_LIMIT = Integer.MAX_VALUE
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f
private const val HORIZONTAL_ELLIPSIS_CHAR = "\u2026"
private const val SINGLE_LINE = 1
private const val DEFAULT_WRAPPED_WIDTH = -1
private const val THREE_SYMBOLS_OFFSET_FROM_TELEGRAM = 3
private const val ADDITIONAL_WIDTH = 300
