package com.example.simpletextview.custom_tools.text_layout.core

import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfig
import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfigurator
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutPadding
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParamsDiff
import com.example.simpletextview.custom_tools.utils.TextHighlights

internal class TextLayoutConfiguratorImpl(params: TextLayoutParams) : TextLayoutConfigurator {

    private var isTextParamsChanged: Boolean = false
    private var isPaddingParamsChanged: Boolean = false
    private var isVisibilityParamsChanged: Boolean = false
    private var isDrawingParamsChanged: Boolean = false

    private var isPaintChanged: Boolean = false
    private var isPaintAlphaChanged: Boolean = false
    private var isPaintLetterSpacingChanged: Boolean = false
    private var isPaintColorChanged: Boolean = false
    private var isPaintTextSizeChanged: Boolean = false

    // region isTextParamsChanged

    override var text: CharSequence = params.text
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    @Px
    override var textSize: Float = params.paint.textSize
        set(value) {
            if (!isPaintTextSizeChanged && field != value) {
                isPaintTextSizeChanged = true
                isTextParamsChanged = true
            }
            field = value
        }

    override var ellipsize: TextUtils.TruncateAt? = params.ellipsize
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var highlights: TextHighlights? = params.highlights
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var alignment: Layout.Alignment = params.alignment
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var includeFontPad: Boolean = params.includeFontPad
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var spacingAdd: Float = params.spacingAdd
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var spacingMulti: Float = params.spacingMulti
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var breakStrategy: Int = params.breakStrategy
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var hyphenationFrequency: Int = params.hyphenationFrequency
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var paint: TextPaint = params.paint
        set(value) {
            isPaintChanged = true
            if (field.textSize != value.textSize) {
                isTextParamsChanged = true
                field = value
                return
            }
            if (field.typeface != value.typeface) {
                isTextParamsChanged = true
                field = value
                return
            }
            if (field.letterSpacing != value.letterSpacing) {
                isTextParamsChanged = true
                field = value
                return
            }
            if (field.alpha != value.alpha) {
                isDrawingParamsChanged = true
                field = value
                return
            }
            if (field.color != value.color) {
                isDrawingParamsChanged = true
                field = value
                return
            }
            field = value
        }

    override var typeface: Typeface? = params.paint.typeface
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var letterSpacing: Float = params.paint.letterSpacing
        set(value) {
            if (!isPaintLetterSpacingChanged && field != value) {
                isPaintLetterSpacingChanged = true
                isTextParamsChanged = true
            }
            field = value
        }

    @Px override var layoutWidth: Int? = params.layoutWidth
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    @Px override var minWidth: Int = params.minWidth
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    @Px override var minHeight: Int = params.minHeight
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    @Px override var maxWidth: Int? = params.maxWidth
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    @Px override var maxHeight: Int? = params.maxHeight
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var maxLines: Int = params.maxLines
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var minLines: Int = params.minLines
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var maxLength: Int = params.maxLength
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    override var needHighWidthAccuracy: Boolean = params.needHighWidthAccuracy
        set(value) {
            if (!isTextParamsChanged) {
                isTextParamsChanged = field != value
            }
            field = value
        }

    // end region isTextParamsChanged

    // region isPaddingParamsChanged

    override var paddingStart: Int = params.padding.start
        set(value) {
            if (!isPaddingParamsChanged) {
                isPaddingParamsChanged = field != value
            }
            field = value
        }

    override var paddingTop: Int = params.padding.top
        set(value) {
            if (!isPaddingParamsChanged) {
                isPaddingParamsChanged = field != value
            }
            field = value
        }

    override var paddingEnd: Int = params.padding.end
        set(value) {
            if (!isPaddingParamsChanged) {
                isPaddingParamsChanged = field != value
            }
            field = value
        }

    override var paddingBottom: Int = params.padding.bottom
        set(value) {
            if (!isPaddingParamsChanged) {
                isPaddingParamsChanged = field != value
            }
            field = value
        }

    // end region isPaddingParamsChanged

    // region isVisibilityParamsChanged

    override var isVisible: Boolean = params.isVisible
        set(value) {
            if (!isVisibilityParamsChanged) {
                isVisibilityParamsChanged = field != value
            }
            field = value
        }

    override var isVisibleWhenBlank: Boolean = params.isVisibleWhenBlank
        set(value) {
            if (!isVisibilityParamsChanged) {
                isVisibilityParamsChanged = field != value
            }
            field = value
        }

    // end region isVisibilityParamsChanged

    // region isDrawingParamsChanged

    @ColorInt override var color: Int = params.paint.color
        set(value) {
            if (!isPaintColorChanged && field != value) {
                isPaintColorChanged = true
                isDrawingParamsChanged = true
            }
            field = value
        }

    override var alpha: Int = params.paint.alpha
        set(value) {
            if (!isPaintAlphaChanged && field != value) {
                isPaintAlphaChanged = true
                isDrawingParamsChanged = true
            }
            field = value
        }

    // end region isDrawingParamsChanged

    inline fun configure(config: TextLayoutConfig): TextLayoutConfiguratorResult {
        config()
        val paint = paint.also {
            if (isPaintColorChanged) it.color = color
            if (isPaintAlphaChanged) it.alpha = alpha
            if (isPaintTextSizeChanged) it.textSize = textSize
            if (isPaintLetterSpacingChanged) it.letterSpacing = letterSpacing
        }
        val params = TextLayoutParams(
            text = text,
            paint = paint,
            alignment = alignment,
            ellipsize = ellipsize,
            includeFontPad = includeFontPad,
            spacingAdd = spacingAdd,
            spacingMulti = spacingMulti,
            breakStrategy = breakStrategy,
            hyphenationFrequency = hyphenationFrequency,
            layoutWidth = layoutWidth,
            minWidth = minWidth,
            minHeight = minHeight,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            maxLines = maxLines,
            minLines = minLines,
            maxLength = maxLength,
            isVisible = isVisible,
            isVisibleWhenBlank = isVisibleWhenBlank,
            highlights = highlights,
            padding = TextLayoutPadding(
                paddingStart,
                paddingTop,
                paddingEnd,
                paddingBottom
            )
        )
        val diff = TextLayoutParamsDiff(
            isTextParamsChanged = isTextParamsChanged,
            isPaddingParamsChanged = isPaddingParamsChanged,
            isVisibilityParamsChanged = isVisibilityParamsChanged,
            isPaintAlphaChanged = isPaintAlphaChanged || isPaintChanged || isPaintColorChanged,
            isDrawingParamsChanged = isDrawingParamsChanged
        )
        return params to diff
    }
}

internal typealias TextLayoutConfiguratorResult = Pair<TextLayoutParams, TextLayoutParamsDiff>