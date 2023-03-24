package com.example.simpletextview.custom_tools.utils

import android.os.Build
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

fun createBoringLayout(
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

fun createStaticLayout(
    text: CharSequence,
    paint: TextPaint,
    width: Int,
    length: Int = text.length,
    alignment: Layout.Alignment,
    spacingMulti: Float = DEFAULT_SPACING_MULTI,
    spacingAdd: Float = DEFAULT_SPACING_ADD,
    includeFontPad: Boolean = false,
    maxLines: Int = 1,
    breakStrategy: Int = 0,
    hyphenationFrequency: Int = 0,
    ellipsize: TextUtils.TruncateAt? = null
): StaticLayout =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(text, 0, length, paint, width)
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
            }.build()
    } else {
        @Suppress("DEPRECATION")
        StaticLayout(
            text,
            0,
            length,
            paint,
            width,
            alignment,
            spacingMulti,
            spacingAdd,
            includeFontPad,
            ellipsize,
            width
        )
    }

private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f