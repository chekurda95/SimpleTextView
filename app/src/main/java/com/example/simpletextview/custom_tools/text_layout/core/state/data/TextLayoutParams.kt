package com.example.simpletextview.custom_tools.text_layout.core.state.data

import android.text.Layout
import android.text.Spannable
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import com.example.simpletextview.custom_tools.utils.TextHighlights
import com.example.simpletextview.custom_tools.utils.getTextWidth
import org.apache.commons.lang3.StringUtils
import kotlin.math.ceil

internal data class TextLayoutParams(
    val text: CharSequence = StringUtils.EMPTY,
    val paint: TextPaint = SimpleTextPaint(),
    @Px val layoutWidth: Int? = null,
    val alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    val ellipsize: TextUtils.TruncateAt? = TextUtils.TruncateAt.END,
    val includeFontPad: Boolean = true,
    val spacingAdd: Float = DEFAULT_SPACING_ADD,
    val spacingMulti: Float = DEFAULT_SPACING_MULTI,
    val maxLines: Int = SINGLE_LINE,
    val minLines: Int = 0,
    val maxLength: Int = Int.MAX_VALUE,
    @Px val minWidth: Int = 0,
    @Px val minHeight: Int = 0,
    @Px val maxWidth: Int? = null,
    @Px val maxHeight: Int? = null,
    val isVisible: Boolean = true,
    val isVisibleWhenBlank: Boolean = true,
    val padding: TextLayoutPadding = TextLayoutPadding(),
    val highlights: TextHighlights? = null,
    val breakStrategy: Int = 0,
    val hyphenationFrequency: Int = 0,
    val canContainUrl: Boolean = false,
    val needHighWidthAccuracy: Boolean = false
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextLayoutParams

        if (text != other.text) return false
        if (paint != other.paint) return false
        if (layoutWidth != other.layoutWidth) return false
        if (alignment != other.alignment) return false
        if (ellipsize != other.ellipsize) return false
        if (includeFontPad != other.includeFontPad) return false
        if (spacingAdd != other.spacingAdd) return false
        if (spacingMulti != other.spacingMulti) return false
        if (maxLines != other.maxLines) return false
        if (minLines != other.minLines) return false
        if (maxLength != other.maxLength) return false
        if (minWidth != other.minWidth) return false
        if (minHeight != other.minHeight) return false
        if (maxWidth != other.maxWidth) return false
        if (maxHeight != other.maxHeight) return false
        if (isVisible != other.isVisible) return false
        if (isVisibleWhenBlank != other.isVisibleWhenBlank) return false
        if (padding != other.padding) return false
        if (highlights != other.highlights) return false
        if (breakStrategy != other.breakStrategy) return false
        if (hyphenationFrequency != other.hyphenationFrequency) return false
        if (canContainUrl != other.canContainUrl) return false
        if (needHighWidthAccuracy != other.needHighWidthAccuracy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + paint.hashCode()
        result = 31 * result + (layoutWidth ?: 0)
        result = 31 * result + alignment.hashCode()
        result = 31 * result + (ellipsize?.hashCode() ?: 0)
        result = 31 * result + includeFontPad.hashCode()
        result = 31 * result + spacingAdd.hashCode()
        result = 31 * result + spacingMulti.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + minLines
        result = 31 * result + maxLength
        result = 31 * result + minWidth
        result = 31 * result + minHeight
        result = 31 * result + (maxWidth ?: 0)
        result = 31 * result + (maxHeight ?: 0)
        result = 31 * result + isVisible.hashCode()
        result = 31 * result + isVisibleWhenBlank.hashCode()
        result = 31 * result + padding.hashCode()
        result = 31 * result + (highlights?.hashCode() ?: 0)
        result = 31 * result + breakStrategy
        result = 31 * result + hyphenationFrequency
        result = 31 * result + canContainUrl.hashCode()
        result = 31 * result + needHighWidthAccuracy.hashCode()
        return result
    }
}

private const val SINGLE_LINE = 1
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f