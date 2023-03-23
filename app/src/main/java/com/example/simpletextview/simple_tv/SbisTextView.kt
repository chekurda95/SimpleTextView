package com.example.simpletextview.simple_tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.text.clearSpans
import androidx.core.view.isGone
import com.example.simpletextview.R
import com.example.simpletextview.custom_tools.text_layout.TextLayout
import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfig
import com.example.simpletextview.custom_tools.utils.MeasureSpecUtils.measureDirection
import com.example.simpletextview.custom_tools.utils.TextLayoutAutoTestsHelper
import com.example.simpletextview.custom_tools.utils.safeRequestLayout
import org.apache.commons.lang3.StringUtils

open class SbisTextView : View, SbisTextViewApi {

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : this(context, attrs, R.attr.sbisTextViewTheme, R.style.SbisTextViewDefaultTheme)

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        @AttrRes defStyleAttr: Int = R.attr.sbisTextViewTheme,
    ) : this(context, attrs, defStyleAttr, R.style.SbisTextViewDefaultTheme)

    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        @AttrRes defStyleAttr: Int = R.attr.sbisTextViewTheme,
        @StyleRes defStyleRes: Int = R.style.SbisTextViewDefaultTheme
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        obtainAttrs(attrs, defStyleAttr, defStyleRes)
    }

    private val textLayout: TextLayout = TextLayout {
        maxLines = DEFAULT_MAX_LINES
        minLines = DEFAULT_MIN_LINES
        ellipsize = null
    }
    private val layoutTouchRect = Rect()

    override var text: CharSequence?
        get() = textLayout.text
        set(value) {
            configure {
                val transformedText = transformationMethod?.getTransformation(value, this@SbisTextView)
                text = transformedText ?: value ?: StringUtils.EMPTY
            }
        }

    @get:Px
    override var textSize: Float
        get() = textLayout.textPaint.textSize
        set(value) {
            configure { paint.textSize = value }
        }

    @get:ColorInt
    override val textColor: Int
        get() = textLayout.textPaint.color

    override val textColors: ColorStateList
        get() = textLayout.colorStateList
            ?: ColorStateList.valueOf(textLayout.textPaint.color)

    override var linkTextColor: Int
        get() = linkTextColors?.defaultColor ?: textColor
        set(value) {
            linkTextColors = ColorStateList.valueOf(value)
        }

    override var linkTextColors: ColorStateList? = null
        set(value) {
            field = value
            updateColors()
        }

    override var allCaps: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            transformationMethod = if (value) AllCapsTransformationMethod() else null
        }

    override var isSingleLine: Boolean
        get() = maxLines == SINGLE_LINE
        set(value) {
            configure {
                maxLines = if (value) SINGLE_LINE else DEFAULT_MAX_LINES
                minLines = DEFAULT_MIN_LINES
                if (ellipsize == null) ellipsize = TextUtils.TruncateAt.END
            }
        }

    override var lines: Int?
        get() = if (maxLines == minLines) maxLines else null
        set(value) {
            configure {
                maxLines = value ?: DEFAULT_MAX_LINES
                minLines = value ?: DEFAULT_MIN_LINES
            }
        }

    override var maxLines: Int?
        get() = textLayout.maxLines
        set(value) {
            configure { maxLines = value ?: DEFAULT_MAX_LINES }
        }

    override var minLines: Int?
        get() = textLayout.minLines
        set(value) {
            configure { minLines = value ?: DEFAULT_MIN_LINES }
        }

    override val lineCount: Int
        get() = layout.lineCount

    override var gravity: Int = Gravity.NO_GRAVITY
        set(value) {
            if (field == value) return
            field = value
            textLayout.configure { alignment = getLayoutAlignment() }
            if (!isGone && isAttachedToWindow) {
                internalLayout()
                invalidate()
            } else {
                safeRequestLayout()
            }
        }

    override var typeface: Typeface?
        get() = paint.typeface
        set(value) {
            configure { paint.typeface = value }
        }

    override var ellipsize: TextUtils.TruncateAt?
        get() = textLayout.ellipsize
        set(value) {
            configure { ellipsize = value }
        }

    override val ellipsizedWidth: Int
        get() = textLayout.ellipsizedWidth

    override var includeFontPadding: Boolean
        get() = textLayout.includeFontPad
        set(value) {
            configure { includeFontPad = value }
        }

    override val paint: TextPaint
        get() = textLayout.textPaint

    override var paintFlags: Int
        get() = paint.flags
        set(value) {
            configure { paint.flags = value }
        }

    override var transformationMethod: TransformationMethod? = null
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged && value != null) {
                val currentText = text
                if (currentText is Spannable) currentText.clearSpans()
                configure { text = value.getTransformation(currentText, this@SbisTextView) }
            }
        }

    @get:IntRange(from = 0, to = 2)
    override var breakStrategy: Int
        get() = textLayout.breakStrategy
        set(value) {
            configure { breakStrategy = value.coerceAtLeast(0) }
        }

    @get:IntRange(from = 0, to = 2)
    override var hyphenationFrequency: Int
        get() = textLayout.hyphenationFrequency
        set(value) {
            configure { hyphenationFrequency = value.coerceAtLeast(0) }
        }

    override val layout: Layout
        get() = textLayout.layout

    init {
        @Suppress("LeakingThis")
        textLayout.makeClickable(this)
        @Suppress("LeakingThis")
        accessibilityDelegate = TextLayoutAutoTestsHelper(this, textLayout)
    }

    override fun setText(@StringRes stringRes: Int) {
        text = resources.getString(stringRes)
    }

    override fun setTextSize(unit: Int, size: Float) {
        val newTextSize = TypedValue.applyDimension(unit, size, resources.displayMetrics)
        textSize = newTextSize
    }

    override fun setTextColor(@ColorInt color: Int) {
        setTextColor(ColorStateList.valueOf(color))
    }

    override fun setTextColor(colorStateList: ColorStateList?) {
        textLayout.colorStateList = colorStateList
        invalidate()
    }

    override fun setTextAppearance(context: Context, @StyleRes style: Int) {
        val attrs = intArrayOf(
            R.styleable.SbisTextView_android_textSize,
            R.styleable.SbisTextView_android_textColor,
            R.styleable.SbisTextView_android_textColorLink,
            R.styleable.SbisTextView_android_textAllCaps,
            R.styleable.SbisTextView_android_textStyle,
            R.styleable.SbisTextView_android_fontFamily
        )

        fun Int.index(): Int = attrs.indexOf(this)

        context.withStyledAttributes(attrs = attrs, resourceId = style) {
            val textSize = getDimension(R.styleable.SbisTextView_android_textSize.index(), 0f)
                .takeIf { it != 0f }
            val colorStateList = getColorStateList(this, R.styleable.SbisTextView_android_textColor.index())
            val color = colorStateList?.defaultColor
                ?: getColor(R.styleable.SbisTextView_android_textColor.index(), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
            val linkColorStateList = getColorStateList(this, R.styleable.SbisTextView_android_textColorLink.index())
            val typeface = getResourceId(R.styleable.SbisTextView_android_fontFamily.index(), NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
                ?.let { ResourcesCompat.getFont(context, it) }
            val textStyle = getInt(R.styleable.SbisTextView_android_textStyle.index(), NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val allCaps = getBoolean(R.styleable.SbisTextView_android_textAllCaps.index(), false)

            var shouldLayout = false
            var shouldInvalidate = false

            textLayout.configure {
                if (textSize != null) {
                    this.paint.textSize = textSize
                    shouldLayout = true
                }
                if (color != null) {
                    this.paint.color = color
                    shouldInvalidate = true
                }
                if (typeface != null || textStyle != null) {
                    this.paint.typeface = textStyle?.let { style ->
                        Typeface.create(typeface ?: this.paint.typeface, style)
                    } ?: typeface
                    shouldLayout = true
                }
            }
            if (colorStateList != null) {
                textLayout.colorStateList = colorStateList
            }
            if (linkColorStateList != null) {
                this@SbisTextView.linkTextColors = linkColorStateList
            }
            if (allCaps) {
                this@SbisTextView.allCaps = true
            }
            when {
                isGone -> Unit
                shouldLayout -> safeRequestLayout()
                shouldInvalidate -> invalidate()
            }
        }
    }

    override fun setLineSpacing(spacingAdd: Float, spacingMulti: Float) {
        configure {
            this.spacingAdd = spacingAdd
            this.spacingMulti = spacingMulti
        }
    }

    override fun setTypeface(typeface: Typeface?, style: Int) {
        if (style > 0) {
            this.typeface = if (typeface == null) {
                Typeface.defaultFromStyle(style)
            } else {
                Typeface.create(typeface, style)
            }
            val typefaceStyle = this.typeface?.style ?: 0
            val need = style and typefaceStyle.inv()
            paint.isFakeBoldText = need and Typeface.BOLD != 0
            paint.textSkewX = (if (need and Typeface.ITALIC != 0) -0.25f else 0f)
        } else {
            paint.isFakeBoldText = false
            paint.textSkewX = 0f
            this.typeface = typeface
        }
    }

    override fun setWidth(@Px width: Int?) {
        configure {
            minWidth = width ?: 0
            maxWidth = width
        }
    }

    override fun setHeight(@Px height: Int?) {
        configure {
            minHeight = height ?: 0
            maxHeight = height
        }
    }

    override fun measureText(): Float =
        paint.measureText(text, 0, text?.length ?: 0)

    override fun measureText(text: CharSequence): Float =
        paint.measureText(text, 0, text.length)

    override fun getEllipsisCount(line: Int): Int =
        textLayout.getEllipsisCount(line)

    override fun setTextAlignment(textAlignment: Int) {
        super.setTextAlignment(textAlignment)
        configure { alignment = getLayoutAlignment() }
    }

    override fun isEnabled(): Boolean =
        textLayout.isEnabled || super.isEnabled()

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        textLayout.isEnabled = enabled
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        textLayout.isSelected = selected
    }

    override fun isSelected(): Boolean =
        textLayout.isSelected || super.isSelected()

    override fun isPressed(): Boolean =
        textLayout.isPressed || super.isPressed()

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        textLayout.isPressed = pressed
    }

    override fun isHorizontalFadingEdgeEnabled(): Boolean =
        textLayout.requiresFadingEdge

    override fun setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled: Boolean) {
        val isChanged = textLayout.requiresFadingEdge != horizontalFadingEdgeEnabled
        textLayout.requiresFadingEdge = horizontalFadingEdgeEnabled
        if (isChanged && textLayout.fadeEdgeSize > 0) safeRequestLayout()
    }

    override fun getHorizontalFadingEdgeLength(): Int =
        textLayout.fadeEdgeSize

    override fun setFadingEdgeLength(length: Int) {
        val rangedValue = length.coerceAtLeast(0)
        val isChanged = textLayout.fadeEdgeSize != rangedValue
        textLayout.fadeEdgeSize = rangedValue
        if (isChanged && textLayout.requiresFadingEdge) safeRequestLayout()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        updateColors()
    }

    override fun getBaseline(): Int =
        getLayoutTop() + textLayout.baseline

    override fun setOnClickListener(listener: OnClickListener?) {
        isClickable = listener != null
        textLayout.setOnClickListener(listener?.let {
            TextLayout.OnClickListener { _, _ -> it.onClick(this) }
        })
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        isLongClickable = listener != null
        textLayout.setOnLongClickListener(listener?.let {
            TextLayout.OnLongClickListener { _, _ -> it.onLongClick(this) }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val layoutTouch = if (isEnabled) textLayout.onTouch(this, event) else false
        val superTouch = super.onTouchEvent(event)
        return layoutTouch || superTouch
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureDirection(widthMeasureSpec) { availableWidth ->
            getInternalSuggestedMinimumWidth(availableWidth)
        }
        val horizontalPadding = paddingStart + paddingEnd
        textLayout.buildLayout { layoutWidth = width - horizontalPadding }
        val height = measureDirection(heightMeasureSpec) { suggestedMinimumHeight }
        setMeasuredDimension(width, height)
    }

    override fun getSuggestedMinimumWidth(): Int =
        getInternalSuggestedMinimumWidth()

    private fun getInternalSuggestedMinimumWidth(availableWidth: Int? = null): Int {
        val horizontalPadding = paddingStart + paddingEnd
        val availableTextWidth = availableWidth?.let { it - horizontalPadding }
        return (horizontalPadding + textLayout.getPrecomputedWidth(availableTextWidth))
            .coerceAtLeast(super.getSuggestedMinimumWidth())
    }

    override fun getSuggestedMinimumHeight(): Int =
        (paddingTop + paddingBottom + textLayout.height)
            .coerceAtLeast(super.getSuggestedMinimumHeight())

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutTouchRect.set(0, 0, w, h)
        textLayout.setStaticTouchRect(layoutTouchRect)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        internalLayout()
    }

    private fun internalLayout() {
        textLayout.layout(paddingStart, getLayoutTop())
    }

    override fun onDraw(canvas: Canvas) {
        textLayout.draw(canvas)
    }

    private fun obtainAttrs(
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int = R.attr.sbisTextViewTheme,
        @StyleRes defStyleRes: Int = R.style.SbisTextViewDefaultTheme
    ) {
        context.withStyledAttributes(attrs, R.styleable.SbisTextView, defStyleAttr, defStyleRes) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveAttributeDataForStyleable(
                    context,
                    R.styleable.SbisTextView,
                    attrs,
                    this,
                    defStyleAttr,
                    defStyleRes
                )
            }
            val text = getText(R.styleable.SbisTextView_android_text) ?: StringUtils.EMPTY
            val textSize = getDimensionPixelSize(R.styleable.SbisTextView_android_textSize, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val colorStateList = getColorStateList(this, R.styleable.SbisTextView_android_textColor)
            val color = colorStateList?.defaultColor
                ?: getColor(R.styleable.SbisTextView_android_textColor, NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                ?: getResourceId(R.styleable.SbisTextView_android_textColor, R.color.black)
                    .let { ContextCompat.getColor(context, it) }
            val linkColorStateList = getColorStateList(this, R.styleable.SbisTextView_android_textColorLink)
            val typeface = getResourceId(R.styleable.SbisTextView_android_fontFamily, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
                ?.let { ResourcesCompat.getFont(context, it) }
            val textStyle = getInt(R.styleable.SbisTextView_android_textStyle, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val includeFontPadding = getBoolean(R.styleable.SbisTextView_android_includeFontPadding, true)
            val allCaps = getBoolean(R.styleable.SbisTextView_android_textAllCaps, false)

            val gravity = getInt(R.styleable.SbisTextView_android_gravity, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val ellipsize = getInt(R.styleable.SbisTextView_android_ellipsize, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
                ?.let { ellipsize ->
                    when (ellipsize) {
                        ELLIPSIZE_END -> TextUtils.TruncateAt.END
                        ELLIPSIZE_START -> TextUtils.TruncateAt.START
                        ELLIPSIZE_MIDDLE -> TextUtils.TruncateAt.MIDDLE
                        ELLIPSIZE_MARQUEE -> TextUtils.TruncateAt.MARQUEE
                        else -> null
                    }
                }
            val breakStrategy = getInt(R.styleable.SbisTextView_android_breakStrategy, 0)
            val hyphenationFrequency = getInt(R.styleable.SbisTextView_android_hyphenationFrequency, 0)
            val isEnabled = getBoolean(R.styleable.SbisTextView_android_enabled, isEnabled)

            val lines = getInt(R.styleable.SbisTextView_android_lines, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val maxLines = getInt(R.styleable.SbisTextView_android_maxLines, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
                ?: DEFAULT_MAX_LINES
            val minLines = getInt(R.styleable.SbisTextView_android_minLines, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
                ?: DEFAULT_MIN_LINES
            val singleLine = getBoolean(R.styleable.SbisTextView_android_singleLine, false)
            val maxLength = getInt(R.styleable.SbisTextView_android_maxLength, Int.MAX_VALUE)

            val minWidth = getDimensionPixelSize(R.styleable.SbisTextView_android_minWidth, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val maxWidth = getDimensionPixelSize(R.styleable.SbisTextView_android_maxWidth, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val minHeight = getDimensionPixelSize(R.styleable.SbisTextView_android_minHeight, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val maxHeight = getDimensionPixelSize(R.styleable.SbisTextView_android_maxHeight, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }

            val requiresFadingEdge = getInt(
                R.styleable.SbisTextView_android_requiresFadingEdge,
                FADING_EDGE_NONE
            ) and FADING_EDGE_HORIZONTAL == FADING_EDGE_HORIZONTAL
            val fadingEdgeLength = getDimensionPixelSize(R.styleable.SbisTextView_android_fadingEdgeLength, 0)

            textLayout.configure {
                this.text = text
                this.paint.also { paint ->
                    paint.textSize = textSize?.toFloat() ?: 0f
                    paint.color = color
                    paint.typeface = textStyle?.let { style ->
                        Typeface.create(typeface, style)
                    } ?: typeface
                }
                this.includeFontPad = includeFontPadding
                this.ellipsize = if (singleLine && ellipsize != null) TextUtils.TruncateAt.END else ellipsize
                this.breakStrategy = breakStrategy
                this.hyphenationFrequency = hyphenationFrequency
                this.maxLines = if (singleLine) SINGLE_LINE else lines ?: maxLines
                this.minLines = if (singleLine) DEFAULT_MIN_LINES else lines ?: minLines
                this.maxLength = maxLength
                if (minWidth != null) this.minWidth = minWidth
                if (maxWidth != null) this.maxWidth = maxWidth
                if (minHeight != null) this.minHeight = minHeight
                if (maxHeight != null) this.maxHeight = maxHeight
            }
            textLayout.apply {
                this.colorStateList = colorStateList
                this.requiresFadingEdge = requiresFadingEdge
                this.fadeEdgeSize = fadingEdgeLength
            }
            this@SbisTextView.linkTextColors = linkColorStateList
            this@SbisTextView.isEnabled = isEnabled
            this@SbisTextView.gravity = gravity ?: Gravity.NO_GRAVITY
            this@SbisTextView.allCaps = allCaps
        }
    }

    private fun getColorStateList(
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

    private fun updateColors() {
        linkTextColors?.getColorForState(drawableState, linkTextColor)?.let { linkColor ->
            if (linkColor != textLayout.textPaint.linkColor) {
                textLayout.textPaint.linkColor = linkColor
                invalidate()
            }
        }
    }

    private fun getLayoutAlignment(): Layout.Alignment =
        when (textAlignment) {
            TEXT_ALIGNMENT_GRAVITY -> {
                when (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
                    Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER
                    Gravity.RIGHT,
                    Gravity.END -> Layout.Alignment.ALIGN_OPPOSITE
                    else -> Layout.Alignment.ALIGN_NORMAL
                }
            }
            TEXT_ALIGNMENT_TEXT_START,
            TEXT_ALIGNMENT_VIEW_START -> Layout.Alignment.ALIGN_NORMAL
            TEXT_ALIGNMENT_TEXT_END,
            TEXT_ALIGNMENT_VIEW_END -> Layout.Alignment.ALIGN_OPPOSITE
            TEXT_ALIGNMENT_CENTER -> Layout.Alignment.ALIGN_CENTER
            else -> Layout.Alignment.ALIGN_NORMAL
        }

    private fun getLayoutTop(): Int =
        when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.BOTTOM -> {
                measuredHeight - paddingBottom - textLayout.height
            }
            Gravity.CENTER, Gravity.CENTER_VERTICAL -> {
                paddingTop + (measuredHeight - paddingTop - paddingBottom - textLayout.height) / 2
            }
            else -> paddingTop
        }

    private fun configure(config: TextLayoutConfig): Boolean =
        textLayout.configure(config).also { isChanged ->
            if (!isGone && isChanged) safeRequestLayout()
        }

    override fun setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled: Boolean) = Unit
    override fun getVerticalFadingEdgeLength(): Int = 0
}

private const val ELLIPSIZE_START = 1
private const val ELLIPSIZE_MIDDLE = 2
private const val ELLIPSIZE_END = 3
private const val ELLIPSIZE_MARQUEE = 4
private const val NO_RESOURCE = -1
private const val SINGLE_LINE = 1
private const val DEFAULT_MIN_LINES = 1
private const val DEFAULT_MAX_LINES = Int.MAX_VALUE
private const val FADING_EDGE_NONE = 0x00000000
private const val FADING_EDGE_HORIZONTAL = 0x00001000