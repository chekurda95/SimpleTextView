package com.example.simpletextview.simple_tv

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.TransformationMethod
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.example.simpletextview.custom_tools.utils.TextHighlights

/**
 * API компонента для отображения текста [SbisTextView].
 *
 * Намеренное отсутствие поддержки:
 * - [TextView.setMovementMethod]
 * - все, что связано со скроллом внутри View.
 *
 * @author vv.chekurda
 */
interface SbisTextViewApi {

    /**
     * Установить текст.
     * @see TextView.setText
     */
    var text: CharSequence?

    /**
     * Установить размер текста в px.
     * @see TextView.setTextSize
     */
    @get:Px
    var textSize: Float

    /**
     * Получить цвет текста.
     * @see setTextColor
     */
    @get:ColorInt
    val textColor: Int

    /**
     * Получить список состояний цвета текста.
     * @see TextView.getTextColors
     * @see setTextColor
     */
    val textColors: ColorStateList

    /**
     * Установить цвет текста для ссылок.
     * @see TextView.setLinkTextColor
     */
    @get:ColorInt
    var linkTextColor: Int

    /**
     * Установить список состояний цвета текста для ссылок.
     * @see TextView.setLinkTextColor
     */
    var linkTextColors: ColorStateList?

    /**
     * Установить необходимость отображать весь текст заглавными буквами.
     * @see TextView.setAllCaps
     */
    var allCaps: Boolean

    /**
     * Установить ограничение текста в одну строчку.
     * @see TextView.setSingleLine
     */
    var isSingleLine: Boolean

    /**
     * Установить количество строк текста.
     * @see TextView.setLines
     */
    var lines: Int?

    /**
     * Установить максимальное количество строк текста.
     * @see TextView.setMaxLines
     */
    var maxLines: Int?

    /**
     * Установить минимальное количество строк текста.
     * @see TextView.setMinLines
     */
    var minLines: Int?

    /**
     * Получить текущее количество строк текста.
     * @see TextView.getLineCount
     * @see lines
     */
    val lineCount: Int

    /**
     * Установить максимальную ширину view.
     * @see TextView.setMaxWidth
     */
    var maxWidth: Int?

    /**
     * Установить минимальную ширину view.
     * @see TextView.setMinWidth
     */
    var minWidth: Int?

    /**
     * Установить максимальную высоту view.
     * @see TextView.setMaxHeight
     */
    var maxHeight: Int?

    /**
     * Установить минимальную высоту view.
     * @see TextView.setMinHeight
     */
    var minHeight: Int?

    /**
     * Установить максимальную длину строки текста.
     */
    var maxLength: Int?

    /**
     * Установить вертикальное/горизонтальное выравнивание текста относительно view.
     * @see TextView.setGravity
     * @see Gravity
     */
    var gravity: Int

    /**
     * Установить шрифт текста.
     * @see TextView.setTypeface
     * @see setTypeface
     */
    var typeface: Typeface?

    /**
     * Установить способ сокращения текста.
     * @see TextView.setEllipsize
     */
    var ellipsize: TextUtils.TruncateAt?

    /**
     * Получить ширину сокращенного текста.
     * @see Layout.getEllipsizedWidth
     */
    val ellipsizedWidth: Int

    /**
     * Установить признак необходимости использования отступов шрифта.
     * @see TextView.setIncludeFontPadding
     */
    var includeFontPadding: Boolean

    /**
     * Получить краску, которой рисуется текст.
     * @see TextView.getPaint
     */
    val paint: TextPaint

    /**
     * Установить флаги краски текста.
     * @see TextView.setPaintFlags
     */
    var paintFlags: Int

    /**
     * Установить метод трансформации текста.
     * @see TextView.setTransformationMethod
     */
    var transformationMethod: TransformationMethod?

    /**
     * Установить стратегию переноса текста.
     * @see TextView.setBreakStrategy
     */
    var breakStrategy: Int

    /**
     * Установить частоту переносов строк.
     * @see TextView.setHyphenationFrequency
     */
    var hyphenationFrequency: Int

    /**
     * Получить текстовую разметку.
     * @see TextView.getLayout
     */
    val layout: Layout

    /**
     * Установить тип автоматического определения размера текста.
     * [AUTO_SIZE_TEXT_TYPE_NONE] - дефолтное значение, автоматическое определение размера текста выключено.
     * [AUTO_SIZE_TEXT_TYPE_UNIFORM] - включить автоматическое определение размера текста.
     */
    @get:AutoSizeTextType
    var autoSizeTextType: Int

    /**
     * Установить максимальное знание размера текста для режима автоматического определения размера текста.
     * @see autoSizeTextType
     */
    @get:Px
    var autoSizeMaxTextSize: Int

    /**
     * Установить минимальное знание размера текста для режима автоматического определения размера текста.
     * @see autoSizeTextType
     */
    @get:Px
    var autoSizeMinTextSize: Int

    /**
     * Установить шаг перебора интервала от минимального до максимального размера текста
     * для режима автоматического определения размера текста.
     * @see autoSizeTextType
     */
    @get:Px
    var autoSizeStepGranularity: Int

    /**
     * Установить максимальный размер текста в пикселях.
     * Использовать для явного ограничения размера при использовании значений sp.
     */
    @get:Px
    var maxTextSize: Int

    /**
     * Установить минимальный размер текста в пикселях.
     * Использовать для явного ограничения размера при использовании значений sp.
     */
    @get:Px
    var minTextSize: Int

    /**
     * Установить текст по строковому ресурсу.
     * @see TextView.setText
     */
    fun setText(@StringRes stringRes: Int)

    /**
     * Установить текст с подсветкой [highlights].
     */
    fun setTextWithHighlights(text: CharSequence?, highlights: TextHighlights?)

    /**
     * Установить размер текста.
     * @see TextView.setTextSize
     */
    fun setTextSize(unit: Int, size: Float)

    /**
     * Установить цвет текста по ресурсу.
     * @see TextView.setTextColor
     */
    fun setTextColor(@ColorInt color: Int)

    /**
     * Установить список состояний цвета текста.
     * @see TextView.setTextColor
     */
    fun setTextColor(colorStateList: ColorStateList?)

    /**
     * Установить стиль текста.
     * @see TextView.setTextAppearance
     */
    fun setTextAppearance(@StyleRes style: Int)

    /**
     * Установить стиль текста.
     * @see TextView.setTextAppearance
     */
    fun setTextAppearance(context: Context, @StyleRes style: Int)

    /**
     * Установить отступы между строк.
     * @see TextView.setLineSpacing
     */
    fun setLineSpacing(
        spacingAdd: Float = DEFAULT_SPACING_ADD,
        spacingMulti: Float = DEFAULT_SPACING_MULTI
    )

    /**
     * Установить шрифт текста со стилем.
     * @see TextView.setTypeface
     */
    fun setTypeface(typeface: Typeface?, style: Int)

    /**
     * Измерить ширину текста [text] в px.
     * Для null производится измерение ширины текущего текста.
     */
    @Px
    fun measureText(text: CharSequence? = null): Float

    /**
     * Получить количество символов, которые попали под сокращение.
     * @see Layout.getEllipsisCount
     */
    fun getEllipsisCount(line: Int): Int

    /**
     * Получить цвет выделения.
     * @see TextView.getHighlightColor
     */
    fun getHighlightColor(): Int
}

private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f