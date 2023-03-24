package com.example.simpletextview.simple_tv

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.TransformationMethod
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

    var text: CharSequence?

    @get:Px
    var textSize: Float

    @get:ColorInt
    val textColor: Int

    val textColors: ColorStateList

    @get:ColorInt
    var linkTextColor: Int

    var linkTextColors: ColorStateList?

    var allCaps: Boolean

    var isSingleLine: Boolean

    var lines: Int?

    var maxLines: Int?

    var minLines: Int?

    val lineCount: Int

    var gravity: Int

    var typeface: Typeface?

    var ellipsize: TextUtils.TruncateAt?

    val ellipsizedWidth: Int

    var includeFontPadding: Boolean

    val paint: TextPaint

    var paintFlags: Int

    var transformationMethod: TransformationMethod?

    var breakStrategy: Int

    var hyphenationFrequency: Int

    val layout: Layout

    fun setText(@StringRes stringRes: Int)

    fun setTextWithHighlights(text: CharSequence?, highlights: TextHighlights?)

    fun setTextSize(unit: Int, size: Float)

    fun setTextColor(@ColorInt color: Int)

    fun setTextColor(colorStateList: ColorStateList?)

    fun setTextAppearance(context: Context, @StyleRes style: Int)

    fun setLineSpacing(
        spacingAdd: Float = DEFAULT_SPACING_ADD,
        spacingMulti: Float = DEFAULT_SPACING_MULTI
    )

    fun setTypeface(typeface: Typeface?, style: Int)

    fun setWidth(@Px width: Int?)

    fun setHeight(@Px height: Int?)

    @Px
    fun measureText(): Float

    @Px
    fun measureText(text: CharSequence): Float

    fun getEllipsisCount(line: Int): Int
}

private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f