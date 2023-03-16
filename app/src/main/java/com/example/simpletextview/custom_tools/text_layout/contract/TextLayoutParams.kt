package com.example.simpletextview.custom_tools.text_layout.contract

import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.text_layout.TextLayout
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import com.example.simpletextview.custom_tools.utils.TextHighlights
import com.example.simpletextview.custom_tools.utils.getTextWidth
import org.apache.commons.lang3.StringUtils
import kotlin.math.ceil

/**
 * Параметры для создания текстовой разметки [Layout] в [TextLayout].
 *
 * @property text текста разметки.
 * @property paint краска текста.
 * @property layoutWidth ширина разметки. Null -> WRAP_CONTENT.
 * @property alignment мод выравнивания текста.
 * @property ellipsize мод сокращения текста.
 * @property includeFontPad включить стандартные отступы шрифта.
 * @property spacingAdd величина межстрочного интервала.
 * @property spacingMulti множитель межстрочного интервала.
 * @property maxLines максимальное количество строк.
 * @property minLines минимальное количество строк.
 * @property maxLength максимальное количество символов в строке.
 * @property isVisible состояние видимости разметки.
 * @property padding внутренние отступы разметки.
 * @property highlights модель для выделения текста.
 * @property minWidth минимальная ширина разметки.
 * @property minHeight минимальная высота разметки.
 * @property maxWidth максимальная ширина разметки.
 * @property maxHeight максимальная высота разметки с учетом [padding]. Необходима для автоматического подсчета [maxLines].
 * @property isVisibleWhenBlank мод скрытия разметки при пустом тексте, включая [padding].
 * @property canContainUrl true, если строка может содержать url. Влияет на точность сокращения текста
 * и скорость создания [StaticLayout]. (Использовать только для [maxLines] > 1, когда текст может содержать ссылки).
 * @property breakStrategy стратегия разрыва строки, см [Layout.BREAK_STRATEGY_SIMPLE].
 * Если необходим только для ссылок, то лучше воспользоваться [canContainUrl].
 * @property hyphenationFrequency частота переноса строк, см. [Layout.HYPHENATION_FREQUENCY_NONE].
 * @property needHighWidthAccuracy true, если необходимо включить мод высокой точности ширины текста.
 * Механика релевантна для однострочных разметок с сокращением текста, к размерам которых привязаны другие элементы.
 * После сокращения текста [StaticLayout] не всегда имеет точные размеры строго по границам текста ->
 * иногда остается лишнее пространство, которое может оказаться критичным для отображения.
 * [needHighWidthAccuracy] решает эту проблему, но накладывает дополнительные расходы на вычисления при перестроении разметки.
 */
data class TextLayoutParams(
    var text: CharSequence = StringUtils.EMPTY,
    var paint: TextPaint = SimpleTextPaint(),
    @Px var layoutWidth: Int? = null,
    var alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    var ellipsize: TextUtils.TruncateAt? = TextUtils.TruncateAt.END,
    var includeFontPad: Boolean = true,
    var spacingAdd: Float = DEFAULT_SPACING_ADD,
    var spacingMulti: Float = DEFAULT_SPACING_MULTI,
    var maxLines: Int = SINGLE_LINE,
    var minLines: Int = 0,
    var maxLength: Int = Int.MAX_VALUE,
    var isVisible: Boolean = true,
    var padding: TextLayoutPadding = TextLayoutPadding(),
    var highlights: TextHighlights? = null,
    @Px var minWidth: Int = 0,
    @Px var minHeight: Int = 0,
    @Px var maxWidth: Int? = null,
    @Px var maxHeight: Int? = null,
    var isVisibleWhenBlank: Boolean = true,
    var canContainUrl: Boolean = false,
    var breakStrategy: Int = 0,
    var hyphenationFrequency: Int = 0,
    var needHighWidthAccuracy: Boolean = false
) {

    /**
     * Ширина текста.
     */
    @get:Px
    internal val textWidth: Int
        get() {
            val layoutWidth = layoutWidth
            return if (layoutWidth != null) {
                maxOf(layoutWidth - padding.start - padding.end, 0)
            } else {
                limitedWidth
            }
        }

    /**
     * Ширина текста с учетом ограничений.
     */
    @get:Px
    internal val limitedWidth: Int
        get() {
            val horizontalPadding = padding.start + padding.end
            val text = configuredText
            val containsAbsoluteSizeSpans = text is Spannable
                    && text.getSpans(0, text.length, AbsoluteSizeSpan::class.java).isNotEmpty()
            val textWidth = if (containsAbsoluteSizeSpans) {
                ceil(Layout.getDesiredWidth(text, paint)).toInt()
            } else {
                paint.getTextWidth(text)
            }
            val minTextWidth = if (minWidth > 0) maxOf(minWidth - horizontalPadding, 0) else 0
            val maxTextWidth = maxWidth?.let { maxOf(it - horizontalPadding, 0) } ?: Integer.MAX_VALUE
            return maxOf(minTextWidth, minOf(textWidth, maxTextWidth))
        }

    /**
     * Максимальная высота текста.
     */
    @get:Px
    internal val textMaxHeight: Int?
        get() = maxHeight?.let { maxOf(it - padding.top - padding.bottom, 0) }

    /**
     * Сконфигурированный текст с учетом настроек параметров.
     */
    internal val configuredText: CharSequence
        get() = when {
            maxLength == Int.MAX_VALUE || maxLength < 0 -> text
            text.isEmpty() -> text
            maxLength >= text.length -> text
            else -> text.subSequence(0, maxLength)
        }

    /**
     * Копировать параметры.
     */
    fun copyParams(): TextLayoutParams = copy(
        paint = SimpleTextPaint().apply {
            typeface = paint.typeface
            textSize = paint.textSize
            color = paint.color
        }
    )
}

private const val SINGLE_LINE = 1
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f