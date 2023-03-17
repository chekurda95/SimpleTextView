package com.example.simpletextview.custom_tools.text_layout
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.text.BoringLayout
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat.ID_NULL
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation
import com.example.simpletextview.BuildConfig
import com.example.simpletextview.custom_tools.TextLayout.Companion.createTextLayoutByStyle
import com.example.simpletextview.custom_tools.styles.CanvasStylesProvider
import com.example.simpletextview.custom_tools.styles.StyleParams.StyleKey
import com.example.simpletextview.custom_tools.styles.StyleParams.TextStyle
import com.example.simpletextview.custom_tools.styles.StyleParamsProvider
import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfig
import com.example.simpletextview.custom_tools.text_layout.contract.TextLayoutConfigurator
import com.example.simpletextview.custom_tools.text_layout.core.TextLayoutConfiguratorImpl
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutPadding
import com.example.simpletextview.custom_tools.utils.getTextWidth
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutDrawableStateHelper
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutFadingEdgeHelper
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutInspectHelper
import com.example.simpletextview.custom_tools.text_layout.core.helpers.TextLayoutTouchHelper
import com.example.simpletextview.custom_tools.text_layout.core.state.TextLayoutState
import com.example.simpletextview.custom_tools.text_layout.core.state.TextLayoutStateReducer
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutDrawParams
import com.example.simpletextview.custom_tools.text_layout.core.state.data.TextLayoutParams
import com.example.simpletextview.custom_tools.utils.LayoutConfigurator
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import timber.log.Timber
import java.lang.Integer.MAX_VALUE
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
    params: TextLayoutParams,
    config: TextLayoutConfig? = null
) : View.OnTouchListener {

    /**
     * @param config настройка параметров текстовой разметки.
     * @see TextLayoutParams
     */
    constructor(config: TextLayoutConfig? = null) : this(TextLayoutParams(), config)

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
                            paddingStart = paddingStyle.paddingStart
                            paddingTop = paddingStyle.paddingTop
                            paddingEnd = paddingStyle.paddingEnd
                            paddingBottom = paddingStyle.paddingBottom
                        }
                    }
                    postConfig?.invoke(this)
                }.apply { colorStateList = style.colorStateList }
            } else TextLayout(postConfig)
    }

    /**
     * Обработчик изменений состояний [state].
     */
    private val reducer = TextLayoutStateReducer()

    /**
     * Состояние компонента [TextLayout].
     */
    private var state: TextLayoutState = reducer.reduceInitialState(params, config)

    private val params: TextLayoutParams by state::params
    private val drawParams: TextLayoutDrawParams by state::drawParams

    /**
     * Вспомогательная реализация для управления рисуемыми состояниями текстовой разметки.
     * @see colorStateList
     */
    private val drawableStateHelper = TextLayoutDrawableStateHelper(state.params.paint)

    /**
     * Вспомогательная реализация для обработки событий касаний по текстовой разметке.
     */
    private val touchHelper by lazy { TextLayoutTouchHelper(this, drawableStateHelper) }

    /**
     * Вспомогательная реализация для отрисовки фэйда вместо многоточия при сокращении текста.
     */
    private val fadingEdgeHelper by lazy { TextLayoutFadingEdgeHelper() }

    /**
     * Вспомогательная реализация для отладки текстовой разметки.
     * Для включения отладочного мода необходимо переключить [isInspectMode] в true.
     * Может оказаться крайне полезным на этапе интеграции [TextLayout].
     */
    private val inspectHelper = if (isInspectMode) TextLayoutInspectHelper() else null

    /**
     * Текущая текстовая разметка.
     * Лениво инициализируется при первом обращении к [TextLayout.layout].
     */
    private var cachedLayout: Layout? = null

    /**
     * Текущая ширина текста без учета оступов.
     * Лениво инициализируется при первом обращении к [layout], если разметка изменилась [isLayoutChanged].
     */
    @Px
    private var cachedTextWidth: Int = 0
        get() = layout.let { field }

    @Px
    private var cachedWidth = 0

    @Px
    private var cachedHeight = 0

    private var cachedIsVisible = true

    /**
     * Признак необходимости в построении layout при следующем обращении
     * по причине изменившихся данных.
     */
    private var isLayoutChanged: Boolean = true
        set(value) {
            field = value
            if (value) {
                isWidthChanged = true
                isHeightChanged = true
                isVisibleChanged = true
            }
        }
    private var isWidthChanged: Boolean = true
    private var isHeightChanged: Boolean = true
    private var isVisibleChanged: Boolean = true

    /**
     * Позиция текста для рисования с учетом внутренних отступов (координата левого верхнего угла).
     */
    private var textPos = params.padding.start.toFloat() to params.padding.top.toFloat()

    /**
     * Прозрачность цвета краски текста.
     */
    private var textColorAlpha = params.paint.alpha

    /**
     * Минимальная высота текста по заданным [TextLayoutParams.minLines].
     */
    @get:Px
    private val minHeightByLines: Int
        get() {
            val layoutHeight = when {
                minLines <= 0 || !isVisible -> 0
                maxLines <= lineCount -> layout.getLineTop(maxLines)
                minLines <= lineCount -> layout.height
                else -> {
                    val lineHeight = with(params) {
                        (paint.getFontMetricsInt(null) * spacingMulti + spacingAdd).roundToInt()
                    }
                    layout.height + (minLines - layout.lineCount) * lineHeight
                }
            }
            return layoutHeight + paddingTop + paddingBottom
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
    val text: CharSequence by params::text

    /**
     * Получить текстовую разметку.
     * Имеет ленивую инициализацию.
     */
    val layout: Layout
        get() {
            val cachedLayout = cachedLayout
            return if (!isLayoutChanged && cachedLayout != null) {
                cachedLayout
            } else {
                updateStaticLayout()
            }
        }

    /**
     * Краска текста разметки.
     */
    val textPaint: TextPaint by params::paint

    /**
     * Видимость разметки.
     */
    val isVisible: Boolean
        get() = if (isVisibleChanged) {
            val result = params.isVisible.let {
                if (!params.isVisibleWhenBlank) it && params.text.isNotBlank()
                else it
            }
            cachedIsVisible = result
            isVisibleChanged = false
            result
        } else cachedIsVisible

    /**
     * Максимальное количество строк.
     */
    val maxLines: Int by params::maxLines

    /**
     * Минимальное количество строк.
     */
    val minLines: Int by params::minLines

    /**
     * Количество строк текста в [TextLayout].
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    val lineCount: Int by layout::lineCount

    /**
     * Левая позиция разметки, установленная в [TextLayout.layout].
     */
    @get:Px
    val left: Int by rect::left

    /**
     * Верхняя позиция разметки, установленная в [TextLayout.layout].
     */
    @get:Px
    val top: Int by rect::top

    /**
     * Правая позиция разметки с учетом внутренних паддингов [left] + [width].
     */
    @get:Px
    val right: Int by rect::right

    /**
     * Нижняя позиция разметки с учетом внутренний паддингов [top] + [height].
     */
    @get:Px
    val bottom: Int by rect::bottom

    /**
     * Левый внутренний оступ разметки.
     */
    @get:Px
    val paddingStart: Int by params.padding::start

    /**
     * Верхний внутренний оступ разметки.
     */
    @get:Px
    val paddingTop: Int by params.padding::top

    /**
     * Првый внутренний оступ разметки.
     */
    @get:Px
    val paddingEnd: Int by params.padding::end

    /**
     * Нижний внутренний оступ разметки.
     */
    @get:Px
    val paddingBottom: Int by params.padding::bottom

    /**
     * Ширина всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val width: Int
        get() = if (isWidthChanged) {
            val result = if (isVisible) {
                params.layoutWidth
                    ?: maxOf(
                        params.minWidth,
                        minOf(
                            paddingStart + cachedTextWidth + paddingEnd,
                            params.maxWidth ?: MAX_VALUE
                        )
                    )
            } else 0
            cachedWidth = result
            isWidthChanged = false
            result
        } else {
            cachedWidth
        }

    /**
     * Высота всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val height: Int
        get() = if (isHeightChanged) {
            val result = when {
                !isVisible -> 0
                width != 0 -> {
                    maxOf(params.minHeight, minHeightByLines)
                        .coerceAtMost(params.maxHeight ?: MAX_VALUE)
                }
                else -> maxOf(params.minHeight, minHeightByLines)
            }
            cachedHeight = result
            isHeightChanged = false
            result
        } else cachedHeight

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
    var layoutAlpha: Float by state::layoutAlpha

    /**
     * Поворот текста вокруг центра на угол в градусах.
     */
    var rotation: Float by drawParams::rotation

    /**
     * Смещение отрисовки разметки по оси X.
     */
    var translationX: Float by drawParams::rotation

    /**
     * Смещение отрисовки разметки по оси Y.
     */
    var translationY: Float by drawParams::rotation

    /**
     * Признак необходимости показа затемнения текста при сокращении.
     */
    var requiresFadingEdge: Boolean by fadingEdgeHelper::requiresFadingEdge

    /**
     * Ширина затенения текста, если он не помещается в разметку.
     */
    var fadeEdgeSize: Int
        get() = fadingEdgeHelper.fadeEdgeSize
        set(value) {
            val isChanged = fadingEdgeHelper.fadeEdgeSize != value
            fadingEdgeHelper.fadeEdgeSize = value
            if (isChanged && value > 0) {
                configure { ellipsize = null }
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
    var colorStateList: ColorStateList? by drawableStateHelper::colorStateList

    /**
     * Установить/получить состояние доступности тестовой разметки.
     *
     * Если текстовая разметка недоступна - клики обрабатываться не будут.
     * @see colorStateList
     * @see makeClickable
     * @see setOnClickListener
     * @see setOnLongClickListener
     */
    var isEnabled: Boolean by drawableStateHelper::isEnabled

    /**
     * Установить/получить нажатое состояние тестовой разметки.
     *
     * @see colorStateList
     */
    var isPressed: Boolean by drawableStateHelper::isPressed

    /**
     * Установить/получить состояние выбранности текстовой разметки.
     *
     * @see colorStateList
     */
    var isSelected: Boolean by drawableStateHelper::isSelected

    /**
     * @see [TextLayoutParams.ellipsize]
     */
    val ellipsize: TruncateAt? by params::ellipsize

    /**
     * @see [Layout.getEllipsizedWidth]
     */
    val ellipsizedWidth: Int by layout::ellipsizedWidth

    /**
     * @see [TextLayoutParams.includeFontPad]
     */
    val includeFontPad: Boolean by params::includeFontPad

    /**
     * @see [TextLayoutParams.breakStrategy]
     */
    val breakStrategy: Int by params::breakStrategy

    /**
     * @see [TextLayoutParams.hyphenationFrequency]
     */
    val hyphenationFrequency: Int by params::hyphenationFrequency

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
    fun copy(config: TextLayoutConfig? = null): TextLayout {
        val newPaint = SimpleTextPaint {
            typeface = textPaint.typeface
            textSize = textPaint.textSize
            color = textPaint.color
            letterSpacing = textPaint.letterSpacing
        }
        return TextLayout(state.params.copy(paint = newPaint), config)
    }

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
        val isChanged = if (isLayoutChanged) {
            TextLayoutConfiguratorImpl(params).apply(config).configure()
            config.invoke(params)
            true
        } else {
            val oldTextSize = params.paint.textSize
            val oldLetterSpacing = params.paint.letterSpacing
            val oldTypeface = params.paint.typeface
            val oldColor = params.paint.color
            val oldParams = params.copy()

            config.invoke(params)
            //checkWarnings()
            if (oldColor != params.paint.color) {
                textColorAlpha = params.paint.alpha
                params.paint.alpha = (textColorAlpha * alpha).toInt()
            }

            val isTextSizeChanged =
                oldTextSize != params.paint.textSize ||
                        oldLetterSpacing != params.paint.letterSpacing ||
                        oldTypeface != params.paint.typeface
            (oldParams != params || isTextSizeChanged).also { isChanged ->
                if (isChanged) isLayoutChanged = true
            }
        }
        drawableStateHelper.textPaint = params.paint
        return isChanged
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
    ): Boolean {
        val isChanged = if (config != null) {
            configure(config)
        } else false
        if (isVisible) layout
        return isChanged
    }

    fun buildLayout(width: Int) {
        if (isLayoutChanged) {
            params.layoutWidth = width
        } else {
            val current = params.layoutWidth
            params.layoutWidth = width
            isLayoutChanged = current != width
        }
        if (isVisible) layout
    }

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
        rect.set(left, top, left + width, top + height)
        textPos = left + paddingStart.toFloat() to top + paddingTop.toFloat()

        touchHelper.updateTouchRect(rect)
        inspectHelper?.updateInfo(layout, rect, textPos)
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

            // TODO("TAGTAG")
            if (fadingEdgeHelper.isFadeEdgeVisible) {
                fadingEdgeHelper.drawFade(canvas, rect) { drawLayout(it, layout) }
            } else {
                drawLayout(canvas, layout)
            }
        }
    }

    private fun drawLayout(canvas: Canvas, layout: Layout) {
        canvas.withClip(rect) {
            canvas.withRotation(rotation, left + width / 2f, top + height / 2f) {
                inspectHelper?.draw(this, isVisible)
                withTranslation(translationX + textPos.first, translationY + textPos.second) {
                    layout.draw(this)
                }
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
        left: Int = touchHelper.touchPaddingLeft,
        top: Int = touchHelper.touchPaddingLeft,
        right: Int = touchHelper.touchPaddingLeft,
        bottom: Int = touchHelper.touchPaddingBottom
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
        touchHelper.setStaticTouchRect(rect ?: this.rect, rect != null)
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

    /**
     * Отменить событие касания.
     */
    fun onTouchCanceled() {
        touchHelper.onTouchCanceled()
    }

    private var boring: BoringLayout.Metrics? = null
    private var savedLayout: BoringLayout? = null

    /**
     * Обновить разметку по набору параметров [params].
     * Если ширина в [params] не задана, то будет использована ширина текста.
     * Созданная разметка помещается в кэш [cachedLayout].
     */
    private fun updateStaticLayout(): Layout {
        if (text !is Spannable) {
            boring = BoringLayout.isBoring(text, textPaint, boring)
        }
        val configurator = LayoutConfigurator(
            params.configuredText,
            params.paint,
            boring,
            savedLayout,
            width = params.textWidth,
            alignment = params.alignment,
            ellipsize = params.ellipsize,
            includeFontPad = params.includeFontPad,
            spacingAdd = params.spacingAdd,
            spacingMulti = params.spacingMulti,
            maxLines = params.maxLines,
            maxHeight = params.textMaxHeight,
            highlights = params.highlights,
            canContainUrl = params.canContainUrl,
            breakStrategy = params.breakStrategy,
            hyphenationFrequency = params.hyphenationFrequency,
            fadingEdge = requiresFadingEdge && fadeEdgeSize > 0
        )
        val layout = configurator.configure()
        layout.also {
            isLayoutChanged = false
            cachedLayout = it
            if (it is BoringLayout) savedLayout = it
            updateCachedTextWidth()
            fadingEdgeHelper.updateFadeEdgeVisibility(params)
        }
        return layout
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
}

/**
 * Мод активации отладочных границ [TextLayout].
 * При включении дополнительно будут нарисованы границы вокруг [TextLayout], а также внутренние отступы.
 */
private const val isInspectMode = false
private const val SINGLE_LINE = 1