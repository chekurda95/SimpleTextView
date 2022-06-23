package com.example.simpletextview.simple_tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.annotation.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.example.simpletextview.R
import com.example.simpletextview.custom_tools.TextLayout
import com.example.simpletextview.custom_tools.styles.CanvasStylesProvider
import com.example.simpletextview.custom_tools.styles.StyleParams
import com.example.simpletextview.custom_tools.styles.StyleParamsProvider
import com.example.simpletextview.custom_tools.styles.getDataFromAttrOrNull
import com.example.simpletextview.custom_tools.utils.safeRequestLayout

open class SimpleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = ResourcesCompat.ID_NULL,
    @StyleRes defStyleRes: Int = ResourcesCompat.ID_NULL,
    styleProvider: StyleParamsProvider<StyleParams.TextStyle>? = SimpleTextViewCanvasStylesProvider.textStyleProvider
) : View(context) {

    /**
     * @see [TextView.setGravity]
     */
    var gravity: Int = Gravity.NO_GRAVITY
        set(value) {
            if (field != value) {
                field = value
                updateGravity()
                internalLayout()
                invalidate()
            }
        }

    /**
     * @see [TextView.setText]
     */
    var text: CharSequence?
        get() = textLayout.text
        set(value) {
            if (text != value) {
                updateConfig(value)
                safeRequestLayout()
            }
        }

    /**
     * @see [TextView.setMaxLines]
     */
    var maxLines: Int
        get() = textLayout.maxLines
        set(value) {
            if (maxLines != value) {
                updateConfig(newMaxLines = value)
                safeRequestLayout()
            }
        }

    var textSize: Float
        get() = textLayout.textPaint.textSize
        set(value) {
            val isChanged = textLayout.configure { paint.textSize = value }
            if (isChanged) safeRequestLayout()
        }

    /** @SelfDocumented */
    var textColors: ColorStateList = ColorStateList.valueOf(Color.WHITE)
        private set

    /**
     * @see [Layout.getEllipsizedWidth]
     */
    val ellipsizedWidth: Int
        get() = textLayout.ellipsizedWidth

    /**
     * @see [TextView.getPaint]
     */
    val paint: TextPaint
        get() = textLayout.textPaint

    private val textLayout: TextLayout

    init {
        val attributes = intArrayOf(
            android.R.attr.id,
            android.R.attr.tag,
            R.attr.SimpleTextView_style
        )
        var styleRes: Int = defStyleRes
        context.withStyledAttributes(attrs, attributes, defStyleAttr, defStyleRes) {
            id = getResourceId(attributes.indexOf(android.R.attr.id), NO_ID)
            tag = getText(attributes.indexOf(android.R.attr.tag))
            styleRes = getResourceId(attributes.indexOf(R.attr.SimpleTextView_style), styleRes)
        }

        textLayout = TextLayout.createTextLayoutByStyle(
            context,
            getTextStyle(defStyleAttr, styleRes),
            styleProvider
        ).apply { makeClickable(this@SimpleTextView) }
    }

    /**
     * @see [TextView.setTextSize]
     */
    fun setTextSize(unit: Int, size: Float) {
        val newTextSize = TypedValue.applyDimension(unit, size, resources.displayMetrics)
        if (newTextSize != textLayout.textPaint.textSize) {
            updateConfig(newTextSize = newTextSize)
            safeRequestLayout()
        }
    }

    /**
     * @see [TextView.setTextColor]
     */
    fun setTextColor(@ColorInt color: Int) {
        textColors = ColorStateList.valueOf(color)
        textLayout.colorStateList = null
        textLayout.textPaint.color = color
        invalidate()
    }

    /**
     * @see [TextView.setTextColor]
     */
    fun setTextColor(colors: ColorStateList) {
        textColors = colors
        textLayout.colorStateList = colors
        invalidate()
    }

    /**
     * @see [TextView.setText]
     */
    fun setText(@StringRes resource: Int) {
        text = resources.getString(resource)
    }

    /**
     * @see [TextView.setTextAppearance]
     */
    fun setTextAppearance(context: Context, @StyleRes style: Int) {
        val styleParams = SimpleTextViewCanvasStylesProvider.textStyleProvider.getStyleParams(context, style)
        var shouldInvalidate = false
        var shouldLayout = false
        textLayout.configure {
            paint.apply {
                styleParams.textColor?.let {
                    if (color != it) {
                        color = it
                        shouldInvalidate = true
                    }
                }
                styleParams.textSize?.let {
                    if (textSize != it) {
                        textSize = it
                        shouldLayout = true
                    }
                }
                styleParams.typeface?.let {
                    if (typeface != it) {
                        typeface = it
                        shouldLayout = true
                    }
                }
            }
        }
        if (shouldLayout) {
            safeRequestLayout()
        } else if (shouldInvalidate) {
            invalidate()
        }
    }

    /**
     * @see [Layout.getEllipsisCount]
     */
    fun getEllipsisCount(line: Int) = textLayout.getEllipsisCount(line)

    override fun isEnabled() = textLayout.isEnabled

    override fun setEnabled(enabled: Boolean) {
        textLayout.isEnabled = enabled
    }

    override fun setSelected(selected: Boolean) {
        textLayout.isSelected = true
    }

    override fun isSelected() = textLayout.isSelected

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth: Int
        val desiredHeight: Int
        if (maxLines > 1) {
            desiredWidth = textLayout.width
            desiredHeight = textLayout.height
        } else {
            desiredWidth = textLayout.getDesiredWidth(text?.toString().orEmpty())
            desiredHeight = textLayout.getDesiredHeight()
        }
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        textLayout.configure { layoutWidth = width }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        internalLayout()
    }

    override fun onDraw(canvas: Canvas) {
        textLayout.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent) =
        (if (isClickable) textLayout.onTouch(this, event) else false) || super.onTouchEvent(event)

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        textLayout.setOnClickListener(l?.let {
            TextLayout.OnClickListener { _, _ ->
                it.onClick(this)
            }
        })
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        textLayout.updatePadding(left, top, right, bottom)
    }

    override fun getPaddingTop() = textLayout.paddingTop

    override fun getPaddingBottom() = textLayout.paddingBottom

    override fun getPaddingLeft() = textLayout.paddingStart

    override fun getPaddingStart() = textLayout.paddingStart

    override fun getPaddingRight() = textLayout.paddingEnd

    override fun getPaddingEnd() = textLayout.paddingEnd

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.text = text
    }

    private fun internalLayout() {
        val actualTop = when (gravity) {
            Gravity.BOTTOM                          -> measuredHeight - textLayout.height
            Gravity.CENTER, Gravity.CENTER_VERTICAL -> (measuredHeight - textLayout.height) / 2
            else                                    -> 0
        }
        textLayout.layout(0, actualTop)
    }

    private fun updateConfig(
        newText: CharSequence? = text,
        newMaxLines: Int = maxLines,
        @Px
        newTextSize: Float = textLayout.textPaint.textSize
    ) {
        textLayout.configure {
            text = newText ?: ""
            paint.textSize = newTextSize
            maxLines = newMaxLines
            padding = TextLayout.TextLayoutPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        }
    }

    private fun updateGravity() {
        textLayout.configure {
            alignment = when (gravity) {
                Gravity.CENTER, Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER
                Gravity.RIGHT, Gravity.END                -> Layout.Alignment.ALIGN_OPPOSITE
                else                                      -> Layout.Alignment.ALIGN_NORMAL
            }
        }
    }

    @StyleRes
    private fun getTextStyle(@AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): Int {
        return context.getDataFromAttrOrNull(defStyleAttr, true) ?: defStyleRes
    }
}

private object SimpleTextViewCanvasStylesProvider : CanvasStylesProvider()