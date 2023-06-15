package com.example.simpletextview.simple_tv

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.example.simpletextview.R

object SbisTextViewObtainHelper {

    private var textAppearanceCache = hashMapOf<Int, TextAppearanceData>()

    internal fun getTextAppearance(
        context: Context,
        currentTypeface: Typeface?,
        @StyleRes style: Int,
    ): TextAppearanceData {
        val cachedStyle = textAppearanceCache[style]
        return if (cachedStyle == null) {
            var data: TextAppearanceData? = null
            context.withStyledAttributes(resourceId = style, attrs = R.styleable.SbisTextView) {
                val textSize = getDimensionPixelSize(R.styleable.SbisTextView_android_textSize, 0)
                    .takeIf { it != 0 }
                val colorStateList = getColorStateList(context, this, R.styleable.SbisTextView_android_textColor)
                val color = colorStateList?.defaultColor
                    ?: getColor(R.styleable.SbisTextView_android_textColor, NO_RESOURCE)
                        .takeIf { it != NO_RESOURCE }
                val linkColorStateList = getColorStateList(context, this, R.styleable.SbisTextView_android_textColorLink)
                val fontFamily = getResourceId(R.styleable.SbisTextView_android_fontFamily, NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                val textStyle = getInt(R.styleable.SbisTextView_android_textStyle, NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                val typeface = getTypeface(context, currentTypeface, fontFamily, textStyle)
                val allCaps = getBoolean(R.styleable.SbisTextView_android_textAllCaps, false)

                data = TextAppearanceData(
                    textSize = textSize?.toFloat(),
                    typeface = typeface,
                    color = color,
                    colorStateList = colorStateList,
                    linkColorStateList = linkColorStateList,
                    allCaps = allCaps
                ).also {
                    textAppearanceCache[style] = it
                }
            }
            requireNotNull(data)
        } else {
            cachedStyle
        }
    }


    internal fun getTypeface(
        context: Context,
        currentTypeface: Typeface?,
        fontFamily: Int?,
        textStyle: Int?,
    ): Typeface? =
        when {
            fontFamily != null -> {
                try {
                    val typeface = ResourcesCompat.getFont(context, fontFamily)
                    if (textStyle != null) {
                        Typeface.create(typeface, textStyle)
                    } else {
                        typeface
                    }
                } catch (ex: Resources.NotFoundException) {
                    // Expected if it is not a font resource.
                    val familyName = context.resources.getString(fontFamily)
                    Typeface.create(familyName, textStyle ?: Typeface.NORMAL)
                }
            }
            textStyle != null -> {
                Typeface.create(currentTypeface, textStyle)
            }
            else -> null
        }

    internal fun getColorStateList(
        context: Context,
        typedArray: TypedArray,
        @StyleableRes attr: Int
    ): ColorStateList? = with(typedArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColorStateList(attr)
        } else {
            getResourceId(attr, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
                ?.let { ContextCompat.getColorStateList(context, it) }
        }
    }

    fun clear() {
        textAppearanceCache.clear()
    }
}

internal data class TextAppearanceData(
    val textSize: Float?,
    val typeface: Typeface?,
    val color: Int?,
    val colorStateList: ColorStateList?,
    val linkColorStateList: ColorStateList?,
    val allCaps: Boolean?
)

private const val NO_RESOURCE = -1