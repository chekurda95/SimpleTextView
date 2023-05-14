package com.example.simpletextview.simple_tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.Layout.Alignment
import android.text.Spannable
import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.text.clearSpans
import androidx.core.view.isGone
import com.example.simpletextview.BuildConfig
import com.example.simpletextview.R
import com.example.simpletextview.custom_tools.TextLayout
import com.example.simpletextview.custom_tools.TextLayoutConfig
import com.example.simpletextview.custom_tools.utils.MeasureSpecUtils.measureDirection
import com.example.simpletextview.custom_tools.utils.TextHighlights
import com.example.simpletextview.custom_tools.utils.getTextWidth
import com.example.simpletextview.custom_tools.utils.safeRequestLayout
import com.example.simpletextview.metrics.Statistic
import org.apache.commons.lang3.StringUtils.EMPTY
import org.json.JSONObject

/**
 * Компонент для отображения текста.
 *
 * Является оптимизированным аналогом [TextView] с сокращенным набором функционала [SbisTextViewApi]
 * и атрибутов [R.styleable.SbisTextView].
 * Компонент может расширяться, поэтому
 * если Вам не хватает какого-то API для вашей интеграции - обратитесь к ответственному за компонент.
 * Приветствуются предложения по переносу в компонент полезных или частоиспользуемых расширений для [TextView],
 * а также заказы на реализацию нового API, которого не хватало в нативном компоненте из коробки.
 *
 * @author vv.chekurda
 */
open class SbisTextView : View, SbisTextViewApi {

    /**
     * Базовый конструктор.
     */
    @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        @AttrRes defStyleAttr: Int = R.attr.sbisTextViewTheme,
        @StyleRes defStyleRes: Int = R.style.SbisTextViewDefaultTheme
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        obtainAttrs(attrs, defStyleAttr, defStyleRes)
    }

    /**
     * Облегченный и самый быстрый конструктор для программного создания [SbisTextView]
     * без темизации с dsl настройкой [config].
     */
    constructor(
        context: Context,
        config: SbisTextViewConfig?
    ) : super(context) {
        applyConfig(config)
    }

    /**
     * Конструктор для программного создания [SbisTextView] с темизированным контекстом по стилю [styleRes].
     * Настройка [config] будет применена поверх атрибутов из стиля.
     */
    constructor(
        context: Context,
        styleRes: Int,
        config: SbisTextViewConfig? = null
    ) : super(ContextThemeWrapper(context, styleRes)) {
        obtainAttrs()
        applyConfig(config)
    }

    private val textLayout: TextLayout = TextLayout {
        maxLines = DEFAULT_MAX_LINES
        minLines = DEFAULT_MIN_LINES
        ellipsize = null
    }.apply {
        makeClickable(this@SbisTextView)
    }
    private val layoutTouchRect = Rect()
    private val descriptionProvider: DescriptionProvider =
        if (BuildConfig.DEBUG) DebugDescriptionProvider()
        else ReleaseDescriptionProvider()

    override var text: CharSequence?
        get() = textLayout.text
        set(value) {
            val isChanged = configure {
                val transformedText = transformationMethod?.getTransformation(value, this@SbisTextView)
                text = transformedText ?: value ?: EMPTY
            }
            if (isChanged) restartForeground()
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
        get() = textLayout.isSingleLine
        set(value) {
            configure {
                isSingleLine = value
                maxLines = if (value) SINGLE_LINE else DEFAULT_MAX_LINES
                minLines = DEFAULT_MIN_LINES
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

    override var maxWidth: Int?
        get() = textLayout.maxWidth
        set(value) {
            configure { maxWidth = value }
        }

    override var minWidth: Int?
        get() = textLayout.minWidth
        set(value) {
            configure { minWidth = value ?: 0 }
        }

    override var maxHeight: Int?
        get() = textLayout.maxHeight
        set(value) {
            configure { maxHeight = value }
        }

    override var minHeight: Int?
        get() = textLayout.minHeight
        set(value) {
            configure { minHeight = value ?: 0 }
        }

    override var maxLength: Int?
        get() = textLayout.maxLength
        set(value) {
            configure { maxLength = value ?: Int.MAX_VALUE }
        }

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

    override var ellipsize: TruncateAt?
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

    /**
     * Установить ширину view в px.
     * @see TextView.setWidth
     */
    @get:JvmName("getViewWidth")
    var width: Int
        get() = super.getWidth()
        set(value) {
            val width = value.takeIf { it >= 0 }
            configure {
                minWidth = width ?: 0
                maxWidth = width
            }
        }

    /**
     * Установить высоту view в px.
     * @see TextView.setHeight
     */
    @get:JvmName("getViewHeight")
    var height: Int
        get() = super.getHeight()
        set(value) {
            val height = value.takeIf { it >= 0 }
            configure {
                minHeight = height ?: 0
                maxHeight = height
            }
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun setText(@StringRes stringRes: Int) {
        text = if (stringRes != 0) {
            resources.getString(stringRes)
        } else {
            EMPTY
        }
    }

    override fun setTextWithHighlights(text: CharSequence?, highlights: TextHighlights?) {
        configure {
            this.text = text ?: EMPTY
            this.highlights = highlights
        }
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

    override fun setTextAppearance(style: Int) {
        setTextAppearance(context, style)
    }

    override fun setTextAppearance(context: Context, @StyleRes style: Int) {
        val textAppearance = SbisTextViewObtainHelper.getTextAppearance(context, typeface, style)
        var shouldLayout = false
        var shouldInvalidate = false

        textLayout.configure {
            if (textAppearance.textSize != null) {
                this.paint.textSize = textAppearance.textSize
                shouldLayout = true
            }
            if (textAppearance.color != null) {
                this.paint.color = textAppearance.color
                shouldInvalidate = true
            }
            if (textAppearance.typeface != null) {
                this.paint.typeface = textAppearance.typeface
                shouldLayout = true
            }
        }
        if (textAppearance.colorStateList != null) {
            textLayout.colorStateList = textAppearance.colorStateList
        }
        if (textAppearance.linkColorStateList != null) {
            this@SbisTextView.linkTextColors = textAppearance.linkColorStateList
        }
        if (textAppearance.allCaps == true) {
            this@SbisTextView.allCaps = true
        }
        when {
            isGone -> Unit
            shouldLayout -> safeRequestLayout()
            shouldInvalidate -> invalidate()
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
            paint.textSkewX = if (need and Typeface.ITALIC != 0) ITALIC_STYLE_PAINT_SKEW else 0f
        } else {
            paint.isFakeBoldText = false
            paint.textSkewX = 0f
            this.typeface = typeface
        }
    }

    override fun measureText(text: CharSequence?): Float {
        val resultText = text ?: this.text ?: return 0f
        return paint.getTextWidth(resultText, 0, resultText.length, byLayout = text is Spannable).toFloat()
    }

    override fun getEllipsisCount(line: Int): Int =
        textLayout.getEllipsisCount(line)

    override fun setTextAlignment(textAlignment: Int) {
        super.setTextAlignment(textAlignment)
        configure { alignment = getLayoutAlignment() }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        textLayout.isEnabled = enabled
    }

    override fun isEnabled(): Boolean =
        textLayout.isEnabled || super.isEnabled()

    override fun dispatchSetSelected(selected: Boolean) {
        textLayout.isSelected = selected
    }

    override fun isSelected(): Boolean =
        textLayout.isSelected || super.isSelected()

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed && isClickable)
    }

    override fun dispatchSetPressed(pressed: Boolean) {
        textLayout.isPressed = pressed
    }

    override fun isPressed(): Boolean =
        textLayout.isPressed || super.isPressed()

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val layoutTouch = if (isEnabled) textLayout.onTouch(this, event) else false
        val superTouch = super.onTouchEvent(event)
        return layoutTouch || superTouch
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val startTime = System.nanoTime()
        val width = measureDirection(widthMeasureSpec) { availableWidth ->
            getInternalSuggestedMinimumWidth(availableWidth)
        }
        val horizontalPadding = paddingStart + paddingEnd
        textLayout.buildLayout(width - horizontalPadding)
        val height = measureDirection(heightMeasureSpec) {
            suggestedMinimumHeight
        }
        setMeasuredDimension(width, height)
        val resultTime = (System.nanoTime() - startTime) / 1000
        Statistic.addSbisMeasureTime(resultTime)
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
        val start = System.nanoTime()
        internalLayout()
        val resultTime = (System.nanoTime() - start) / 1000
        Statistic.addSbisLayoutTime(resultTime)
    }

    private fun internalLayout() {
        textLayout.layout(paddingStart, getLayoutTop())
    }

    override fun onDraw(canvas: Canvas) {
        textLayout.draw(canvas)
    }

    private fun obtainAttrs(
        attrs: AttributeSet? = null,
        @AttrRes defStyleAttr: Int = 0,
        @StyleRes defStyleRes: Int = 0
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
            val textAppearance = getResourceId(R.styleable.SbisTextView_android_textAppearance, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val textAppearanceData = if (textAppearance != null) {
                SbisTextViewObtainHelper.getTextAppearance(context, typeface, textAppearance)
            } else {
                null
            }
            val text = getText(R.styleable.SbisTextView_android_text) ?: EMPTY
            val textSize = getDimensionPixelSize(R.styleable.SbisTextView_android_textSize, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
                ?: textAppearanceData?.textSize
            val colorStateList = SbisTextViewObtainHelper.getColorStateList(
                context,
                this,
                R.styleable.SbisTextView_android_textColor
            ) ?: textAppearanceData?.colorStateList
            val color = colorStateList?.defaultColor
                ?: getColor(R.styleable.SbisTextView_android_textColor, NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                ?: getResourceId(R.styleable.SbisTextView_android_textColor, NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                    ?.let { ContextCompat.getColor(context, it) }
                ?: textAppearanceData?.color
                ?: ContextCompat.getColor(context, R.color.black)
            val linkColorStateList = SbisTextViewObtainHelper.getColorStateList(
                context,
                this,
                R.styleable.SbisTextView_android_textColorLink
            ) ?: textAppearanceData?.linkColorStateList
            val fontFamily = getResourceId(R.styleable.SbisTextView_android_fontFamily, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val textStyle = getInt(R.styleable.SbisTextView_android_textStyle, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val typeface =
                if (fontFamily == null && textStyle == null) {
                    textAppearanceData?.typeface ?: paint.typeface
                } else {
                    SbisTextViewObtainHelper.getTypeface(
                        context,
                        textAppearanceData?.typeface,
                        fontFamily,
                        textStyle
                    )
                }

            val includeFontPadding = getBoolean(R.styleable.SbisTextView_android_includeFontPadding, true)
            val allCaps = getBoolean(R.styleable.SbisTextView_android_textAllCaps, false)

            val gravity = getInt(R.styleable.SbisTextView_android_gravity, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val ellipsize = getInt(R.styleable.SbisTextView_android_ellipsize, NO_RESOURCE)
                .takeIf { it != NO_RESOURCE }
            val truncateAt = ellipsize?.let {
                when (ellipsize) {
                    ELLIPSIZE_NONE -> null
                    ELLIPSIZE_END -> TruncateAt.END
                    ELLIPSIZE_START -> TruncateAt.START
                    ELLIPSIZE_MIDDLE -> TruncateAt.MIDDLE
                    ELLIPSIZE_MARQUEE -> TruncateAt.MARQUEE
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
                    paint.typeface = typeface
                }
                this.includeFontPad = includeFontPadding
                this.ellipsize = if (singleLine && ellipsize == null) TruncateAt.END else truncateAt
                this.breakStrategy = breakStrategy
                this.hyphenationFrequency = hyphenationFrequency
                this.maxLines = if (singleLine) SINGLE_LINE else lines ?: maxLines
                this.minLines = if (singleLine) DEFAULT_MIN_LINES else lines ?: minLines
                this.isSingleLine = singleLine
                this.maxLength = maxLength
                if (minWidth != null) this.minWidth = minWidth
                if (maxWidth != null) this.maxWidth = maxWidth
                if (minHeight != null) this.minHeight = minHeight
                if (maxHeight != null) this.maxHeight = maxHeight
            }
            textLayout.also {
                it.colorStateList = colorStateList
                it.requiresFadingEdge = requiresFadingEdge
                it.fadeEdgeSize = fadingEdgeLength
            }
            this@SbisTextView.also {
                it.linkTextColors = linkColorStateList
                it.isEnabled = isEnabled
                it.gravity = gravity ?: Gravity.NO_GRAVITY
                it.allCaps = allCaps
            }
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

    private fun getLayoutAlignment(): Alignment =
        when (textAlignment) {
            TEXT_ALIGNMENT_GRAVITY -> {
                when (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
                    Gravity.CENTER_HORIZONTAL -> Alignment.ALIGN_CENTER
                    Gravity.RIGHT,
                    Gravity.END -> Alignment.ALIGN_OPPOSITE
                    else -> Alignment.ALIGN_NORMAL
                }
            }
            TEXT_ALIGNMENT_TEXT_START,
            TEXT_ALIGNMENT_VIEW_START -> Alignment.ALIGN_NORMAL
            TEXT_ALIGNMENT_TEXT_END,
            TEXT_ALIGNMENT_VIEW_END -> Alignment.ALIGN_OPPOSITE
            TEXT_ALIGNMENT_CENTER -> Alignment.ALIGN_CENTER
            else -> Alignment.ALIGN_NORMAL
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

    private fun applyConfig(config: SbisTextViewConfig?) {
        config?.invoke(this)
    }

    private fun restartForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground?.setVisible(false, true)
        }
    }

    override fun setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled: Boolean) = Unit
    override fun getVerticalFadingEdgeLength(): Int = 0

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.text = text
    }

    override fun onPopulateAccessibilityEvent(event: AccessibilityEvent) {
        super.onPopulateAccessibilityEvent(event)
        if (!text.isNullOrEmpty()) {
            event.text.add(text)
        }
    }

    @SuppressLint("GetContentDescriptionOverride")
    override fun getContentDescription(): CharSequence =
        descriptionProvider.getContentDescription()

    private inner class ReleaseDescriptionProvider : DescriptionProvider {
        override fun getContentDescription(): CharSequence =
            text ?: EMPTY
    }

    private inner class DebugDescriptionProvider : DescriptionProvider {
        override fun getContentDescription(): CharSequence =
            JSONObject().apply {
                put(DESCRIPTION_TEXT_KEY, text)
                put(DESCRIPTION_TEXT_SIZE_KEY, textSize)
                put(
                    DESCRIPTION_TEXT_COLOR_KEY,
                    String.format(COLOR_HEX_STRING_FORMAT, paint.color and 0xFFFFFF).uppercase()
                )
                put(DESCRIPTION_ELLIPSIZE_KEY, ellipsize?.toString() ?: NONE_VALUE)
                if (maxLines != DEFAULT_MAX_LINES) put(DESCRIPTION_MAX_LINES_KEY, maxLines)
                if (minLines != DEFAULT_MIN_LINES) put(DESCRIPTION_MIN_LINES_KEY, minLines)
            }.toString()
    }
}

private interface DescriptionProvider {
    fun getContentDescription(): CharSequence
}

/**
 * Настройка параметров [SbisTextView].
 */
typealias SbisTextViewConfig = SbisTextView.() -> Unit

private const val ELLIPSIZE_NONE = 0
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
private const val ITALIC_STYLE_PAINT_SKEW = -0.25f

private const val DESCRIPTION_TEXT_KEY = "text"
private const val DESCRIPTION_TEXT_SIZE_KEY = "text_size"
private const val DESCRIPTION_TEXT_COLOR_KEY = "text_color"
private const val DESCRIPTION_MAX_LINES_KEY = "max_lines"
private const val DESCRIPTION_MIN_LINES_KEY = "min_lines"
private const val DESCRIPTION_ELLIPSIZE_KEY = "ellipsize"
private const val NONE_VALUE = "none"
private const val COLOR_HEX_STRING_FORMAT = "#%06x"