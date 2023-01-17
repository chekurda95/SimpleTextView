package com.example.simpletextview.custom_tools

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.Layout
import android.text.Layout.Alignment
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt
import android.text.style.AbsoluteSizeSpan
import android.view.GestureDetector
import android.view.HapticFeedbackConstants.LONG_PRESS
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewConfiguration.getPressedStateDuration
import androidx.annotation.AttrRes
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat.ID_NULL
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation
import com.example.simpletextview.BuildConfig
import org.apache.commons.lang3.StringUtils
import com.example.simpletextview.custom_tools.TextLayout.*
import com.example.simpletextview.custom_tools.TextLayout.Companion.createTextLayoutByStyle
import com.example.simpletextview.custom_tools.styles.CanvasStylesProvider
import com.example.simpletextview.custom_tools.styles.StyleParams.StyleKey
import com.example.simpletextview.custom_tools.styles.StyleParams.TextStyle
import com.example.simpletextview.custom_tools.styles.StyleParamsProvider
import com.example.simpletextview.custom_tools.utils.StaticLayoutConfigurator
import com.example.simpletextview.custom_tools.utils.TextHighlights
import com.example.simpletextview.custom_tools.utils.getTextWidth
import com.example.simpletextview.custom_tools.*
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import timber.log.Timber
import java.lang.Integer.MAX_VALUE
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Разметка для отображения текста.
 *
 * @param config настройка параметров текстовой разметки.
 * @see TextLayoutParams
 *
 * Является оберткой над [Layout] для отображения текста,
 * который лениво создается по набору параметров модели [params].
 * Также содержит параметры и api ускоряющие и облегчающие работу с кастомной текстовой разметкой.
 *
 * Параметры разметки настраиваются с помощью конфига [TextLayoutConfig] в конструкторе,
 * или с помощью методов [configure] и [buildLayout].
 * Статичный метод [createTextLayoutByStyle] позволяет создавать разметку по xml стилю.
 *
 * Дополнительный функционал:
 * - Установка слушателя кликов [TextLayout.OnClickListener].
 * - Установка долгих кликов кликов [TextLayout.OnLongClickListener].
 * - Менеджер для упрошения обработки касаний по текстовым разметкам [TextLayoutTouchManager].
 * - Поддержка состояний [isEnabled], [isPressed], [isSelected], см. [colorStateList].
 * - Поддержка информации для автотестов кастомных [View] на базе [TextLayout] с помощью [TextLayoutAutoTestsHelper].
 * - Отладка границ разметки при помощи локального включения [isInspectMode], см. [InspectHelper].
 *
 * @author vv.chekurda
 */
class TextLayout private constructor(
    private val params: TextLayoutParams,
    config: TextLayoutConfig? = null
) : View.OnTouchListener {

    /**
     * @param config настройка параметров текстовой разметки.
     * @see TextLayoutParams
     */
    constructor(config: TextLayoutConfig? = null) : this(TextLayoutParams(), config)

    init { config?.invoke(params) }

    /**
     * Слушатель кликов по текстовой разметке.
     * @see TextLayout.setOnClickListener
     */
    fun interface OnClickListener {

        fun onClick(context: Context, layout: TextLayout)
    }

    /**
     * Слушатель долгих кликов по текстовой разметке.
     * @see TextLayout.setOnLongClickListener
     */
    fun interface OnLongClickListener {

        fun onLongClick(context: Context, layout: TextLayout)
    }

    companion object {

        /**
         * Создать текстовую разметку [TextLayout] по параметрам ресурса стиля [styleRes].
         *
         * @param styleProvider поставщик стилей [TextStyle].
         * @param obtainPadding true, если текстовая разметка должна получить отступы из стиля.
         * @param postConfig конфиг параметров текстовой разметки
         * для дополнительной настройки после инициализии из ресурса стиля.
         */
        fun createTextLayoutByStyle(
            context: Context,
            @StyleRes styleRes: Int,
            styleProvider: StyleParamsProvider<TextStyle>? = null,
            obtainPadding: Boolean = true,
            postConfig: TextLayoutConfig? = null
        ): TextLayout =
            createTextLayoutByStyle(context, StyleKey(styleRes), styleProvider, obtainPadding, postConfig)

        /**
         * Создать текстовую разметку [TextLayout] по ключу стиля [styleKey].
         * @see StyleKey
         *
         * Использовать для сценариев, когда значения атрибутов стиля [StyleKey.styleRes] могут зависеть от разных тем,
         * поэтому для правильного кэширования помимо ресурса стиля необходим дополнительный [StyleKey.tag].
         *
         * @param styleProvider поставщик стилей [TextStyle].
         * @param obtainPadding true, если текстовая разметка должна получить отступы из стиля.
         * @param postConfig конфиг параметров текстовой разметки
         * для дополнительной настройки после инициализии из ресурса стиля.
         */
        fun createTextLayoutByStyle(
            context: Context,
            styleKey: StyleKey,
            styleProvider: StyleParamsProvider<TextStyle>? = null,
            obtainPadding: Boolean = true,
            postConfig: TextLayoutConfig? = null
        ): TextLayout =
            if (styleKey.styleRes != 0) {
                val style = styleProvider?.getStyleParams(context, styleKey)
                    ?: CanvasStylesProvider.obtainTextStyle(context, styleKey)
                TextLayout {
                    paint = SimpleTextPaint {
                        textSize = style.textSize ?: textSize
                        color = style.textColor ?: color
                        typeface = style.typeface ?: typeface
                    }
                    text = style.text ?: text
                    layoutWidth = style.layoutWidth.takeIf { it != 0 } ?: layoutWidth
                    alignment = style.alignment ?: alignment
                    ellipsize = style.ellipsize ?: ellipsize
                    includeFontPad = style.includeFontPad ?: includeFontPad
                    maxLines = style.maxLines ?: maxLines
                    isVisible = style.isVisible ?: isVisible
                    if (obtainPadding) {
                        style.paddingStyle?.also { paddingStyle ->
                            padding = TextLayoutPadding(
                                paddingStyle.paddingStart,
                                paddingStyle.paddingTop,
                                paddingStyle.paddingEnd,
                                paddingStyle.paddingBottom
                            )
                        }
                    }
                    postConfig?.invoke(this)
                }.apply { colorStateList = style.colorStateList }
            } else TextLayout(postConfig)
    }

    /**
     * Вспомогательный класс для обработки событий касаний по текстовой разметке.
     */
    private val touchHelper: TouchHelper by lazy { TouchHelper() }

    /**
     * Горизонтальный паддинг для обработки касаний (левый и правый).
     */
    private val horizontalTouchPadding: Pair<Int, Int>
        get() = touchHelper.horizontalTouchPadding

    /**
     * Вертикальный паддинг для обработки касаний (верхний и нижний).
     */
    private val verticalTouchPadding: Pair<Int, Int>
        get() = touchHelper.verticalTouchPadding

    /**
     * Вспомогательный класс для управления рисуемыми состояниями текстовой разметки.
     * @see colorStateList
     */
    private val drawableStateHelper: DrawableStateHelper by lazy { DrawableStateHelper() }

    /**
     * Вспомогательный класс для отладки текстовой разметки.
     * Для включения отладочного мода необходимо переключить [isInspectMode] в true.
     * Может оказаться крайне полезным на этапе интеграции [TextLayout].
     */
    private val inspectHelper = if (isInspectMode) InspectHelper() else null

    /**
     * Получить снимок состояния [TextLayout].
     */
    internal val state: TextLayoutState
        get() = TextLayoutState(
            params.copy(),
            cachedLayout,
            isLayoutChanged,
            textPos
        )

    /**
     * Текущая текстовая разметка.
     * Лениво инициализируется при первом обращении к [TextLayout.layout].
     */
    private var cachedLayout: Layout? = null

    /**
     * Признак необходимости затенения каря текста, когда он не помещается в рзметку
     */
    private var isFadeEdgeVisible: Boolean = false

    /**
     * Текущая ширина текста без учета оступов.
     * Лениво инициализируется при первом обращении к [layout], если разметка изменилась [isLayoutChanged].
     */
    @Px
    private var cachedTextWidth: Int = 0
        get() = layout.let { field }

    /**
     * Признак необходимости в построении layout при следующем обращении
     * по причине изменившихся данных.
     */
    private var isLayoutChanged: Boolean = true

    /**
     * Позиция текста для рисования с учетом внутренних отступов (координата левого верхнего угла).
     */
    private var textPos = params.padding.start.toFloat() to params.padding.top.toFloat()

    /**
     * Прозрачность цвета краски текста.
     */
    private var textColorAlpha = textPaint.alpha

    /**
     * Минимальная высота текста по заданным [TextLayoutParams.minLines].
     */
    @get:Px
    private val minHeightByLines: Int
        get() {
            val layoutHeight = when {
                minLines <= 0 || !isVisible -> 0
                minLines <= layout.lineCount -> layout.height
                else -> {
                    val lineHeight = with(params) {
                        (paint.getFontMetricsInt(null) * spacingMulti + spacingAdd).roundToInt()
                    }
                    layout.height + (minLines - layout.lineCount) * lineHeight
                }
            }
            return layoutHeight + paddingTop + paddingBottom
        }

    private val fadeMatrix by lazy { Matrix() }

    private val fadePaint by lazy {
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
    }

    private var fadeShader: Lazy<Shader>? = null

    private fun createFadeShader(): Lazy<Shader> = lazy {
        LinearGradient(
            0f,
            0f,
            fadeEdgeSize.toFloat(),
            0f,
            Color.TRANSPARENT,
            Color.WHITE,
            Shader.TileMode.CLAMP
        ).also {
            fadePaint.shader = it
        }
    }

    /**
     * Координаты границ [TextLayout], полученные в [TextLayout.layout].
     */
    private var rect = Rect()

    /**
     * Идентификатор разметки.
     */
    @IdRes
    var id: Int = ID_NULL

    /**
     * Текст разметки.
     */
    val text: CharSequence
        get() = params.text

    /**
     * Получить текстовую разметку.
     * Имеет ленивую инициализацию.
     */
    val layout: Layout
        get() = cachedLayout
            ?.takeIf { !isLayoutChanged }
            ?: updateStaticLayout()

    /**
     * Краска текста разметки.
     */
    val textPaint: TextPaint
        get() = params.paint

    /**
     * Видимость разметки.
     */
    val isVisible: Boolean
        get() = params.isVisible.let {
            if (!params.isVisibleWhenBlank) it && params.text.isNotBlank()
            else it
        }

    /**
     * Максимальное количество строк.
     */
    val maxLines: Int
        get() = params.maxLines

    /**
     * Минимальное количество строк.
     */
    val minLines: Int
        get() = params.minLines

    /**
     * Количество строк текста в [TextLayout].
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    val lineCount: Int
        get() = layout.lineCount

    /**
     * Левая позиция разметки, установленная в [TextLayout.layout].
     */
    @get:Px
    val left: Int
        get() = rect.left

    /**
     * Верхняя позиция разметки, установленная в [TextLayout.layout].
     */
    @get:Px
    val top: Int
        get() = rect.top

    /**
     * Правая позиция разметки с учетом внутренних паддингов [left] + [width].
     */
    @get:Px
    val right: Int
        get() = rect.right

    /**
     * Нижняя позиция разметки с учетом внутренний паддингов [top] + [height].
     */
    @get:Px
    val bottom: Int
        get() = rect.bottom

    /**
     * Левый внутренний оступ разметки.
     */
    @get:Px
    val paddingStart: Int
        get() = params.padding.start

    /**
     * Верхний внутренний оступ разметки.
     */
    @get:Px
    val paddingTop: Int
        get() = params.padding.top

    /**
     * Првый внутренний оступ разметки.
     */
    @get:Px
    val paddingEnd: Int
        get() = params.padding.end

    /**
     * Нижний внутренний оступ разметки.
     */
    @get:Px
    val paddingBottom: Int
        get() = params.padding.bottom

    /**
     * Ширина всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val width: Int
        get() = if (isVisible) {
            params.layoutWidth
                ?: maxOf(
                    params.minWidth,
                    minOf(paddingStart + cachedTextWidth + paddingEnd, params.maxWidth ?: MAX_VALUE)
                )
        } else 0

    /**
     * Высота всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val height: Int
        get() = when {
            !isVisible -> 0
            width != 0 -> {
                maxOf(params.minHeight, minHeightByLines)
                    .coerceAtMost(params.maxHeight ?: MAX_VALUE)
            }
            else -> maxOf(params.minHeight, minHeightByLines)
        }

    /**
     * Базовая линия текстовой разметки.
     */
    @get:Px
    val baseline: Int
        get() = paddingTop + (cachedLayout?.getLineBaseline(0) ?: 0)

    /**
     * Прозрачность текста разметки.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    var alpha: Float = 1f
        set(value) {
            field = value
            textPaint.alpha = (value * textColorAlpha).toInt()
        }

    /**
     * Поворот текста вокруг центра на угол в градусах.
     */
    var rotation = 0f

    /**
     * Смещение отрисовки разметки по оси X.
     */
    var translationX: Float = 0f

    /**
     * Смещение отрисовки разметки по оси Y.
     */
    var translationY: Float = 0f

    /**
     * Признак необходимости показа затемнения текста при сокращении.
     */
    var requiresFadingEdge: Boolean = false

    /**
     * Ширина затенения текста, если он не помещается в разметку.
     */
    var fadeEdgeSize: Int = 0
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) {
                fadeShader = createFadeShader()
                if (value > 0) configure { ellipsize = null }
            }
        }

    /**
     * Установить/получить список цветов текста для состояний.
     * @see isEnabled
     * @see isPressed
     * @see isSelected
     *
     * Для работы [ColorStateList] необходимо сделать разметку кликабельной [makeClickable],
     * а также доставлять события касаний с помощью [TextLayoutTouchManager] или самостоятельно в метод [onTouch].
     */
    var colorStateList: ColorStateList? = null
        set(value) {
            val isChanged = value != field
            field = value
            if (isChanged) drawableStateHelper.onColorStateListChanged()
        }

    /**
     * Установить/получить состояние доступности тестовой разметки.
     *
     * Если текстовая разметка недоступна - клики обрабатываться не будут.
     * @see colorStateList
     * @see makeClickable
     * @see setOnClickListener
     * @see setOnLongClickListener
     */
    var isEnabled: Boolean = true
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) drawableStateHelper.setEnabled(value)
        }

    /**
     * Установить/получить нажатое состояние тестовой разметки.
     *
     * @see colorStateList
     */
    var isPressed: Boolean = false
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) drawableStateHelper.setPressed(value)
        }

    /**
     * Установить/получить состояние выбранности текстовой разметки.
     *
     * @see colorStateList
     */
    var isSelected: Boolean = false
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) drawableStateHelper.setSelected(value)
        }

    /**
     * @see [TextLayoutParams.ellipsize]
     */
    val ellipsize: TruncateAt?
        get() = params.ellipsize

    /**
     * @see [Layout.getEllipsizedWidth]
     */
    val ellipsizedWidth: Int
        get() = layout.ellipsizedWidth

    /**
     * @see [TextLayoutParams.includeFontPad]
     */
    val includeFontPad: Boolean
        get() = params.includeFontPad

    /**
     * @see [TextLayoutParams.breakStrategy]
     */
    val breakStrategy: Int
        get() = params.breakStrategy

    /**
     * @see [TextLayoutParams.hyphenationFrequency]
     */
    val hyphenationFrequency: Int
        get() = params.hyphenationFrequency

    /**
     * Получить ожидаемую ширину разметки для однострочного текста [text] без создания [StaticLayout].
     * По-умолчанию используется текст из параметров рамзетки [TextLayoutParams.text].
     */
    @Px
    fun getDesiredWidth(text: CharSequence? = null): Int {
        val resultText = text ?: params.text
        return paddingStart + params.paint.getTextWidth(resultText) + paddingEnd
    }

    /**
     * Получить ожидаемую высоту разметки для однострочного текста без создания [StaticLayout].
     */
    @Px
    fun getDesiredHeight(): Int = textPaint.fontMetrics.let {
        (it.bottom - it.top + it.leading).roundToInt() + paddingTop + paddingBottom
    }

    /**
     * Измерить ширину разметки с учетом ограничений:
     * - [TextLayoutParams.maxWidth]
     * - [TextLayoutParams.minWidth]
     * - [TextLayoutParams.maxLength]
     */
    @Px
    fun measureWidth(): Int =
        paddingStart + params.limitedWidth + paddingEnd

    /**
     * Копировать текстовую разметку c текущими [params].
     * @param config настройка параметров текстовой разметки.
     * @return новый текстовая разметка с копированными параметрами текущей разметки.
     */
    fun copy(config: TextLayoutConfig? = null): TextLayout =
        TextLayout(params.copyParams(), config)

    /**
     * Настроить разметку.
     * Если параметры изменятся - разметка будет построена при следующем обращении.
     *
     * Использовать для изменения закэшированных параметров [params],
     * созданных при инициализации или переданных ранее,
     * кэш статичной разметки при этом будет обновлен по новым параметрам при следующем обращении.
     *
     * @param config настройка параметров текстовой разметки.
     * @return true, если параметры изменились.
     */
    fun configure(
        config: TextLayoutConfig
    ): Boolean {
        val oldTextSize = params.paint.textSize
        val oldLetterSpacing = params.paint.letterSpacing
        val oldTypeface = params.paint.typeface
        val oldColor = params.paint.color
        val oldParams = params.copy()

        config.invoke(params)
        checkWarnings()
        if (oldColor != params.paint.color) {
            textColorAlpha = params.paint.alpha
            params.paint.alpha = (textColorAlpha * alpha).toInt()
        }

        val isTextSizeChanged =
            oldTextSize != params.paint.textSize ||
                    oldLetterSpacing != params.paint.letterSpacing ||
                    oldTypeface != params.paint.typeface
        return (oldParams != params || isTextSizeChanged).also { isChanged ->
            if (isChanged) isLayoutChanged = true
        }
    }

    /**
     * Построить разметку.
     *
     * Использовать для принудительного построения разметки на базе параметров [params],
     * при этом настройка [config] будет применена перед построением новой разметки.
     *
     * @param config настройка параметров текстовой разметки.
     * @return true, если разметка изменилась.
     */
    fun buildLayout(
        config: TextLayoutConfig? = null
    ): Boolean =
        config?.let { configure(it) }
            .also { if (isVisible) layout }
            ?: false

    /**
     * Обновить внутренние отступы.
     *
     * @return true, если отступы изменились.
     */
    fun updatePadding(
        start: Int = paddingStart,
        top: Int = paddingTop,
        end: Int = paddingEnd,
        bottom: Int = paddingBottom
    ): Boolean = with(params) {
        val oldPadding = padding
        padding = TextLayoutPadding(start, top, end, bottom)
        isLayoutChanged = oldPadding != padding || isLayoutChanged
        oldPadding != padding
    }

    /**
     * Разместить разметку на координате ([left], [top]).
     * Координата является позицией левого верхнего угла [TextLayout]
     *
     * Метод вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    fun layout(@Px left: Int, @Px top: Int) {
        rect.set(
            left,
            top,
            left + width,
            top + height
        )
        textPos = left + paddingStart.toFloat() to top + paddingTop.toFloat()

        touchHelper.updateTouchRect()
        inspectHelper?.updatePositions()
    }

    /**
     * Нарисовать разметку.
     *
     * Рисуется именно кэш текстовой разметки [cachedLayout],
     * чтобы не допускать построения layout на [View.onDraw].
     */
    fun draw(canvas: Canvas) {
        cachedLayout?.let { layout ->
            if (!isVisible || params.text.isEmpty()) return

            if (isFadeEdgeVisible) {
                drawFade(canvas) { drawLayout(it, layout) }
            } else {
                drawLayout(canvas, layout)
            }
        }
    }

    private fun drawFade(canvas: Canvas, function: (Canvas) -> Unit) {
        val saveCount = canvas.saveLayer(0f, 0f, right.toFloat(), bottom.toFloat(), null)
        val fadeLeft = right.toFloat() - fadeEdgeSize
        function(canvas)
        fadeMatrix.reset()
        fadeMatrix.postTranslate(fadeLeft, 0f)
        fadeShader?.value?.setLocalMatrix(fadeMatrix)
        canvas.drawRect(
            fadeLeft,
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            fadePaint
        )
        canvas.restoreToCount(saveCount)
    }

    private fun drawLayout(canvas: Canvas, layout: Layout) {
        canvas.withRotation(rotation, left + width / 2f, top + height / 2f) {
            inspectHelper?.draw(this)
            withTranslation(translationX + textPos.first, translationY + textPos.second) {
                layout.draw(this)
            }
        }
    }

    /**
     * Сделать текстовую разметку кликабельной.
     * @param parentView view, в которой находится текстовая разметка.
     *
     * Необходимо вызывать для включения обработки [onTouch].
     * @see TextLayoutTouchManager - менеджер, который автоматически включает кликабельность.
     */
    fun makeClickable(parentView: View) {
        touchHelper.init(parentView)
        drawableStateHelper.init(parentView)
    }

    /**
     * Установить слушателя кликов [listener] по текстовой разметке.
     * @see TextLayoutTouchManager
     *
     * Для включения обработки кликов разметка должна быть кликабельная [makeClickable].
     * В состоянии [isEnabled] == false - клики обрабатываться не будут.
     */
    fun setOnClickListener(listener: OnClickListener?) {
        touchHelper.setOnClickListener(listener)
    }

    /**
     * Установить слушателя долгих кликов [listener] по текстовой разметке.
     * @see TextLayoutTouchManager
     *
     * Для включения обработки долгих кликов разметка должна быть кликабельная [makeClickable].
     * В состоянии [isEnabled] == false - клики обрабатываться не будут.
     */
    fun setOnLongClickListener(listener: OnLongClickListener?) {
        touchHelper.setOnLongClickListener(listener)
    }

    /**
     * Установить отступы для увеличения области касания по текстовой разметке.
     *
     * Отступы будут применены к основным границам [TextLayout] после вызова [TextLayout.layout].
     * Фактически происходит расширение кликабельной области на заданные значения
     * и не влияет на размер и позиции разметки.
     */
    fun setTouchPadding(
        left: Int = horizontalTouchPadding.first,
        top: Int = verticalTouchPadding.first,
        right: Int = horizontalTouchPadding.second,
        bottom: Int = horizontalTouchPadding.second
    ) {
        touchHelper.setTouchPadding(left, top, right, bottom)
    }

    /**
     * Установить отступы [padding] по всему периметру для увеличения области касания по текстовой разметке.
     * @see setTouchPadding
     */
    fun setTouchPadding(padding: Int) {
        touchHelper.setTouchPadding(padding)
    }

    /**
     * Установить статичную область кликабельности текстовой разметки.
     *
     * При установке [rect] отступы из [setTouchPadding] перестанут работать.
     * Для сброса статичной области кликабельности необходимо передать [rect] == null.
     */
    fun setStaticTouchRect(rect: Rect?) {
        touchHelper.setStaticTouchRect(rect)
    }

    /**
     * @see [Layout.getEllipsisCount]
     */
    fun getEllipsisCount(line: Int): Int =
        if (maxLines == SINGLE_LINE) {
            params.text.count() - layout.text.count()
        } else {
            layout.getEllipsisCount(line)
        }

    /**
     * Обработать событие касания.
     *
     * Для включения обработки событий касания необходимо сделать текстовую разметку кликабельной [makeClickable].
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean =
        touchHelper.onTouch(event)
            ?.also { isHandled -> drawableStateHelper.checkPressedState(event.action, isHandled) }
            ?.takeIf { touchHelper.onClickListener != null || touchHelper.onLongClickListener != null }
            ?: false

    /**
     * Отменить событие касания.
     */
    fun onTouchCanceled() {
        drawableStateHelper.checkPressedState(ACTION_CANCEL, true)
    }

    /**
     * Обновить разметку по набору параметров [params].
     * Если ширина в [params] не задана, то будет использована ширина текста.
     * Созданная разметка помещается в кэш [cachedLayout].
     */
    private fun updateStaticLayout(): Layout =
        StaticLayoutConfigurator.createStaticLayout(params.configuredText, params.paint) {
            width = params.textWidth
            alignment = params.alignment
            ellipsize = params.ellipsize
            includeFontPad = params.includeFontPad
            spacingAdd = params.spacingAdd
            spacingMulti = params.spacingMulti
            maxLines = params.maxLines
            maxHeight = params.textMaxHeight
            highlights = params.highlights
            canContainUrl = params.canContainUrl
            breakStrategy = params.breakStrategy
            hyphenationFrequency = params.hyphenationFrequency
            fadingEdge = requiresFadingEdge && fadeEdgeSize > 0
        }.also {
            isLayoutChanged = false
            cachedLayout = it
            updateCachedTextWidth()
            updateFadeEdgeVisibility()
        }

    /**
     * Обновить кэш ширины текста без учета отступов [cachedTextWidth].
     */
    private fun updateCachedTextWidth() {
        cachedTextWidth = if (layout.lineCount == SINGLE_LINE && params.needHighWidthAccuracy) {
            layout.getLineWidth(0).roundToInt()
        } else {
            layout.width
        }
    }

    /**
     * Обновить признак затенения каря для слишком длинного текста [isFadeEdgeVisible].
     */
    private fun updateFadeEdgeVisibility() {
        isFadeEdgeVisible = requiresFadingEdge && fadeEdgeSize > 0
                && maxLines == 1
                && params.text != TextUtils.ellipsize(
            text,
            textPaint,
            params.textWidth.toFloat(),
            TruncateAt.END
        )
    }

    private fun checkWarnings() {
        val layoutWidth = params.layoutWidth
        if (!BuildConfig.DEBUG || layoutWidth == null) return

        val minWidth = params.minWidth
        val maxWidth = params.maxWidth
        if (minWidth > 0 && layoutWidth < minWidth) {
            Timber.e(
                IllegalArgumentException(
                    "Потенциальная ошибка отображения TextLayout: " +
                            "значение параметра layoutWidth(${params.layoutWidth}) меньше minWidth(${params.minWidth}). " +
                            "Приоритетное значение размера - layoutWidth(${params.layoutWidth}). TextLayoutParams = $params"
                )
            )
        }
        if (maxWidth != null && layoutWidth > maxWidth) {
            Timber.e(
                IllegalArgumentException(
                    "Потенциальная ошибка отображения TextLayout: " +
                            "значение параметра layoutWidth(${params.layoutWidth}) больше maxWidth(${params.maxWidth}). " +
                            "Приоритетное значение размера - layoutWidth(${params.layoutWidth}). TextLayoutParams = $params"
                )
            )
        }
    }

    /**
     * Параметры для создания текстовой разметки [Layout] в [TextLayout].
     *
     * @property text текста разметки.
     * @property paint краска текста.
     * @property layoutWidth ширина разметки. Null -> WRAP_CONTENT.
     * @property alignment мод выравнивания текста.
     * @property ellipsize мод сокращения текста.
     * @property includeFontPad включить стандартные отступы шрифта.
     * @property spacingAdd величина межстрочного интервала.
     * @property spacingMulti множитель межстрочного интервала.
     * @property maxLines максимальное количество строк.
     * @property minLines минимальное количество строк.
     * @property maxLength максимальное количество символов в строке.
     * @property isVisible состояние видимости разметки.
     * @property padding внутренние отступы разметки.
     * @property highlights модель для выделения текста.
     * @property minWidth минимальная ширина разметки.
     * @property minHeight минимальная высота разметки.
     * @property maxWidth максимальная ширина разметки.
     * @property maxHeight максимальная высота разметки с учетом [padding]. Необходима для автоматического подсчета [maxLines].
     * @property isVisibleWhenBlank мод скрытия разметки при пустом тексте, включая [padding].
     * @property canContainUrl true, если строка может содержать url. Влияет на точность сокращения текста
     * и скорость создания [StaticLayout]. (Использовать только для [maxLines] > 1, когда текст может содержать ссылки).
     * @property breakStrategy стратегия разрыва строки, см [Layout.BREAK_STRATEGY_SIMPLE].
     * Если необходим только для ссылок, то лучше воспользоваться [canContainUrl].
     * @property hyphenationFrequency частота переноса строк, см. [Layout.HYPHENATION_FREQUENCY_NONE].
     * @property needHighWidthAccuracy true, если необходимо включить мод высокой точности ширины текста.
     * Механика релевантна для однострочных разметок с сокращением текста, к размерам которых привязаны другие элементы.
     * После сокращения текста [StaticLayout] не всегда имеет точные размеры строго по границам текста ->
     * иногда остается лишнее пространство, которое может оказаться критичным для отображения.
     * [needHighWidthAccuracy] решает эту проблему, но накладывает дополнительные расходы на вычисления при перестроении разметки.
     */
    data class TextLayoutParams(
        var text: CharSequence = StringUtils.EMPTY,
        var paint: TextPaint = SimpleTextPaint(),
        @Px var layoutWidth: Int? = null,
        var alignment: Alignment = Alignment.ALIGN_NORMAL,
        var ellipsize: TruncateAt? = TruncateAt.END,
        var includeFontPad: Boolean = true,
        var spacingAdd: Float = DEFAULT_SPACING_ADD,
        var spacingMulti: Float = DEFAULT_SPACING_MULTI,
        var maxLines: Int = SINGLE_LINE,
        var minLines: Int = 0,
        var maxLength: Int = Int.MAX_VALUE,
        var isVisible: Boolean = true,
        var padding: TextLayoutPadding = TextLayoutPadding(),
        var highlights: TextHighlights? = null,
        @Px var minWidth: Int = 0,
        @Px var minHeight: Int = 0,
        @Px var maxWidth: Int? = null,
        @Px var maxHeight: Int? = null,
        var isVisibleWhenBlank: Boolean = true,
        var canContainUrl: Boolean = false,
        var breakStrategy: Int = 0,
        var hyphenationFrequency: Int = 0,
        var needHighWidthAccuracy: Boolean = false
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
                val maxTextWidth = maxWidth?.let { maxOf(it - horizontalPadding, 0) } ?: MAX_VALUE
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

        /**
         * Копировать параметры.
         */
        fun copyParams(): TextLayoutParams = copy(
            paint = SimpleTextPaint().apply {
                typeface = paint.typeface
                textSize = paint.textSize
                color = paint.color
            }
        )
    }

    /**
     * Параметры отступов текстовой разметки [Layout] в [TextLayout].
     */
    data class TextLayoutPadding(
        @Px val start: Int = 0,
        @Px val top: Int = 0,
        @Px val end: Int = 0,
        @Px val bottom: Int = 0
    )

    /**
     * Вспомогательный класс для обработки касаний по [TextLayout].
     */
    private inner class TouchHelper {

        private var parentView: View? = null

        private val touchRect: Rect = Rect()
        private var isStaticTouchRect = false
        var horizontalTouchPadding = 0 to 0
            private set
        var verticalTouchPadding = 0 to 0
            private set

        private var gestureDetector: GestureDetector? = null
            get() {
                if (field == null) {
                    field = parentView?.context?.let {
                        object : GestureDetector(it, gestureListener) {
                            override fun onTouchEvent(ev: MotionEvent): Boolean =
                                if (ev.action == ACTION_MOVE && drawableStateHelper.isPressedState) {
                                    gestureListener.onMove(ev)
                                } else {
                                    super.onTouchEvent(ev)
                                }
                        }
                    }
                }
                field?.setIsLongpressEnabled(onLongClickListener != null)
                return field
            }

        private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

            private fun isInTouchRect(event: MotionEvent) =
                touchRect.contains(event.x.roundToInt(), event.y.roundToInt())

            override fun onDown(event: MotionEvent): Boolean =
                isInTouchRect(event)

            override fun onSingleTapUp(event: MotionEvent): Boolean =
                (isInTouchRect(event)).also { isConfirmed ->
                    if (!isEnabled || !isConfirmed) return@also

                    val context = parentView?.context ?: return@also
                    onClickListener?.onClick(context, this@TextLayout)
                }

            override fun onLongPress(event: MotionEvent) {
                if (isInTouchRect(event) && isEnabled) {
                    val context = parentView?.context ?: return
                    onLongClickListener?.onLongClick(context, this@TextLayout)?.also {
                        parentView?.performHapticFeedback(LONG_PRESS)
                    }
                }
            }

            fun onMove(event: MotionEvent) = isInTouchRect(event)
        }
        var onClickListener: OnClickListener? = null
            private set
        var onLongClickListener: OnLongClickListener? = null
            private set

        /**
         * Проинициализировать помощника.
         *
         * @param parentView view, в которой находится текстовая разметка.
         */
        fun init(parentView: View) {
            this.parentView = parentView
        }

        /**
         * Установить слушателя кликов [listener] по текстовой разметке.
         * @see TextLayoutTouchManager
         *
         * Для включения обработки кликов разметка должна быть кликабельная [makeClickable].
         * В состоянии [isEnabled] == false - клики обрабатываться не будут.
         */
        fun setOnClickListener(listener: OnClickListener?) {
            onClickListener = listener
        }

        /**
         * Установить слушателя долгих кликов [listener] по текстовой разметке.
         * @see TextLayoutTouchManager
         *
         * Для включения обработки долгих кликов разметка должна быть кликабельная [makeClickable].
         * В состоянии [isEnabled] == false - клики обрабатываться не будут.
         */
        fun setOnLongClickListener(listener: OnLongClickListener?) {
            onLongClickListener = listener
        }

        /**
         * Установить отступы для увеличения области касания по текстовой разметке.
         *
         * Отступы будут применены к основным границам [TextLayout] после вызова [TextLayout.layout].
         * Фактически происходит расширение кликабельной области на заданные значения
         * и не влияет на размер и позиции разметки.
         */
        fun setTouchPadding(
            left: Int = horizontalTouchPadding.first,
            top: Int = verticalTouchPadding.first,
            right: Int = horizontalTouchPadding.second,
            bottom: Int = horizontalTouchPadding.second
        ) {
            horizontalTouchPadding = left to right
            verticalTouchPadding = top to bottom
        }

        /**
         * Установить отступы [padding] по всему периметру для увеличения области касания по текстовой разметке.
         * @see setTouchPadding
         */
        fun setTouchPadding(padding: Int) {
            setTouchPadding(left = padding, top = padding, right = padding, bottom = padding)
        }

        /**
         * Установить статичную область кликабельности текстовой разметки.
         *
         * При установке [rect] отступы из [setTouchPadding] перестанут работать.
         * Для сброса статичной области кликабельности необходимо передать [rect] == null.
         */
        fun setStaticTouchRect(rect: Rect?) {
            touchRect.set(rect ?: this@TextLayout.rect)
            isStaticTouchRect = touchRect != this@TextLayout.rect
        }

        /**
         * Обновить область касания согласно [TextLayout.rect].
         *
         * Игнорируется, если установлена статичная область касания [setStaticTouchRect].
         */
        fun updateTouchRect() {
            if (isStaticTouchRect) return
            with(rect) {
                touchRect.set(
                    left - horizontalTouchPadding.first,
                    top - verticalTouchPadding.first,
                    right + horizontalTouchPadding.second,
                    bottom + verticalTouchPadding.second
                )
            }
        }

        /**
         * Обработать событие касания [event].
         * @return true, если событие касания было обработано текущей текстовой разметкой.
         */
        fun onTouch(event: MotionEvent): Boolean? =
            gestureDetector?.onTouchEvent(event)
    }

    /**
     * Вспомогательный класс для управления рисуемыми состояниями текстовой разметки.
     * @see colorStateList
     */
    private inner class DrawableStateHelper {

        /**
         * Список текущих рисуемых состояний текстовой разметки.
         */
        private val drawableState = mutableSetOf(android.R.attr.state_enabled)
        private var parentView: View? = null

        /**
         * Получить состояние нажатости.
         */
        val isPressedState: Boolean
            get() = drawableState.contains(android.R.attr.state_pressed)

        /**
         * Проинициализировать помощника.
         *
         * @param parentView view, в которой находится текстовая разметка.
         */
        fun init(parentView: View) {
            this.parentView = parentView
        }

        /**
         * Колбэк об обновлении списка цветов для состояний - [colorStateList].
         */
        fun onColorStateListChanged() {
            updateTextColorByState()
        }

        /**
         * Проверить состояние нажатости по действию события касания [motionAction] и признаку обработки этого события [isHandled].
         */
        fun checkPressedState(motionAction: Int, isHandled: Boolean) {
            when (motionAction and ACTION_MASK) {
                ACTION_DOWN,
                ACTION_POINTER_DOWN -> {
                    if (isHandled) {
                        removeCancelPressedCallback()
                        if (isEnabled) setPressed(true)
                    }
                }
                ACTION_UP -> dispatchCancelPressedCallback()
                ACTION_CANCEL -> setPressed(false)
            }
        }

        /**
         * Установить доступное состояние тестовой разметки.
         */
        fun setEnabled(enabled: Boolean) {
            val enabledAttr = android.R.attr.state_enabled
            val disableAttr = -enabledAttr

            val isStateChanged = if (enabled) {
                val isAdded = drawableState.add(enabledAttr)
                val isRemoved = drawableState.remove(disableAttr)
                isAdded || isRemoved
            } else {
                val isAdded = drawableState.add(disableAttr)
                val isRemoved = drawableState.remove(enabledAttr)
                isPressed = false
                isAdded || isRemoved
            }

            if (isStateChanged) {
                updateTextColorByState()
                invalidate()
            }
        }

        /**
         * Установить нажатое состояние тестовой разметки.
         */
        fun setPressed(pressed: Boolean) {
            updateDrawableState(android.R.attr.state_pressed, pressed)
        }

        /**
         * Установить выбранное состояние текстовой разметки.
         */
        fun setSelected(selected: Boolean) {
            updateDrawableState(android.R.attr.state_selected, selected)
        }

        /**
         * Обновить рисуемое состояние текстовой разметки.
         *
         * @param stateAttr атрибут нового состояния
         * @param isActive true, если состояние активно
         */
        private fun updateDrawableState(@AttrRes stateAttr: Int, isActive: Boolean) {
            val isStateChanged =
                if (isActive) drawableState.add(stateAttr)
                else drawableState.remove(stateAttr)

            if (isStateChanged) {
                updateTextColorByState()
                invalidate()
            }
        }

        /**
         * Обновить цвет текста согласно текущему рисуемому состоянию.
         */
        private fun updateTextColorByState() {
            textPaint.drawableState = drawableState.toIntArray()
            colorStateList?.let { stateList ->
                textPaint.color = stateList.getColorForState(textPaint.drawableState, stateList.defaultColor)
            }
        }

        private fun invalidate() {
            parentView?.takeIf { colorStateList != null && it.isAttachedToWindow }
                ?.invalidate()
        }

        /**
         * Действие отмены нажатого рисуемого состояния.
         */
        private val cancelPressedCallback = Runnable { setPressed(false) }

        /**
         * Отправить отложенное действие [cancelPressedCallback] для отмены нажатого рисуемого состояния.
         */
        private fun dispatchCancelPressedCallback() {
            parentView?.handler?.postDelayed(
                cancelPressedCallback,
                getPressedStateDuration().toLong()
            )
        }

        /**
         * Очистить колбэк для отмены нажатого рисуемого состояния [cancelPressedCallback].
         */
        private fun removeCancelPressedCallback() {
            parentView?.handler?.removeCallbacks(cancelPressedCallback)
        }
    }

    /**
     * Вспомогательный класс для отладки текстовой разметки.
     * Позволяет отображать границы [TextLayout], а также внутренние отступы.
     * Может оказаться крайне полезным на этапе интеграции [TextLayout].
     */
    private inner class InspectHelper {

        /**
         * Краска линии границы по периметру [TextLayout].
         */
        val borderPaint = Paint(ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.STROKE
        }

        /**
         * Краска внутренних отступов [TextLayout].
         */
        val paddingPaint = Paint(ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
        }
        val borderPath = Path()
        val borderRectF = RectF()
        val paddingPath = Path()
        val textBackgroundPath = Path()

        /**
         * Обновить закэшированные позиции границ разметки.
         */
        fun updatePositions() {
            borderPath.reset()
            textBackgroundPath.reset()
            paddingPath.reset()

            borderRectF.set(
                left.toFloat() + ONE_PX,
                top.toFloat() + ONE_PX,
                right.toFloat() - ONE_PX,
                bottom.toFloat() - ONE_PX
            )
            borderPath.addRect(borderRectF, Path.Direction.CW)

            textBackgroundPath.addRect(
                textPos.first,
                textPos.second,
                textPos.first + layout.width,
                textPos.second + layout.height,
                Path.Direction.CW
            )
            paddingPath.addRect(borderRectF, Path.Direction.CW)
            paddingPath.op(textBackgroundPath, Path.Op.DIFFERENCE)
        }

        /**
         * Нарисовать отладочные границы разметки.
         */
        fun draw(canvas: Canvas) {
            if (isVisible) {
                canvas.drawPath(paddingPath, paddingPaint)
                canvas.drawPath(borderPath, borderPaint)
            }
        }
    }

    /**
     * Модель внутреннего состояния [TextLayout].
     * @see TextLayout.params
     * @see TextLayout.cachedLayout
     * @see TextLayout.isLayoutChanged
     * @see TextLayout.textPos
     */
    internal data class TextLayoutState(
        val params: TextLayoutParams,
        val cachedLayout: Layout?,
        val isLayoutChanged: Boolean,
        val textPos: Pair<Float, Float>
    )
}

/**
 * Настройка для параметров [TextLayout.TextLayoutParams].
 */
typealias TextLayoutConfig = TextLayoutParams.() -> Unit

/**
 * Мод активации отладочных границ [TextLayout].
 * При включении дополнительно будут нарисованы границы вокруг [TextLayout], а также внутренние отступы.
 */
private const val isInspectMode = false
private const val ONE_PX = 1
private const val SINGLE_LINE = 1
private const val DEFAULT_SPACING_ADD = 0f
private const val DEFAULT_SPACING_MULTI = 1f