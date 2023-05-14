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

object SbisTextViewObtainHelper {

    private var textAppearanceCache = hashMapOf<Int, TextAppearanceData>()

    private val textAppearanceAttrs by lazy(LazyThreadSafetyMode.NONE) {
        intArrayOf(
            android.R.attr.textSize,
            android.R.attr.textColor,
            android.R.attr.textColorLink,
            android.R.attr.textAllCaps,
            android.R.attr.textStyle,
            android.R.attr.fontFamily
        )
    }

    internal fun getTextAppearance(
        context: Context,
        currentTypeface: Typeface?,
        @StyleRes style: Int,
    ): TextAppearanceData {
        val cachedStyle = textAppearanceCache[style]
        return if (cachedStyle == null) {
            fun Int.index(): Int = textAppearanceAttrs.indexOf(this)
            var data: TextAppearanceData? = null
            context.withStyledAttributes(attrs = textAppearanceAttrs, resourceId = style) {
                val textSize = getDimensionPixelSize(android.R.attr.textSize.index(), 0)
                    .takeIf { it != 0 }
                val colorStateList = getColorStateList(context, this, android.R.attr.textColor.index())
                val color = colorStateList?.defaultColor
                    ?: getColor(android.R.attr.textColor.index(), NO_RESOURCE)
                        .takeIf { it != NO_RESOURCE }
                val linkColorStateList = getColorStateList(context, this, android.R.attr.textColorLink.index())
                val fontFamily = getResourceId(android.R.attr.fontFamily.index(), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                val textStyle = getInt(android.R.attr.textStyle.index(), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                val typeface = getTypeface(context, currentTypeface, fontFamily, textStyle)
                val allCaps = getBoolean(android.R.attr.textAllCaps.index(), false)

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
                    ResourcesCompat.getFont(context, fontFamily)
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