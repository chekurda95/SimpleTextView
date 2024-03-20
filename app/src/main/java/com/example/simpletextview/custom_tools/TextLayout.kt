package com.example.simpletextview.custom_tools

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.BoringLayout
import android.text.Layout
import android.text.Layout.Alignment
import android.text.Spannable
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import android.text.style.AbsoluteSizeSpan
import android.text.style.MetricAffectingSpan
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants.LONG_PRESS
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewConfiguration.getPressedStateDuration
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat.ID_NULL
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation
import com.example.simpletextview.custom_tools.styles.CanvasStylesProvider
import com.example.simpletextview.custom_tools.styles.StyleParams.StyleKey
import com.example.simpletextview.custom_tools.styles.StyleParams.TextStyle
import com.example.simpletextview.custom_tools.styles.StyleParamsProvider
import com.example.simpletextview.custom_tools.utils.SimpleTextPaint
import com.example.simpletextview.custom_tools.utils.TextHighlights
import com.example.simpletextview.custom_tools.utils.getTextWidth
import com.example.simpletextview.custom_tools.utils.layout.LayoutConfigurator
import org.apache.commons.lang3.StringUtils
import com.example.simpletextview.custom_tools.TextLayout.*
import com.example.simpletextview.custom_tools.TextLayout.Companion.createTextLayoutByStyle
import com.example.simpletextview.custom_tools.utils.layout.LayoutCreator
import com.example.simpletextview.custom_tools.utils.layout.LayoutFactory
import com.example.simpletextview.custom_tools.utils.layout.LayoutFactory.Companion.RTL_SYMBOLS_CHECK_COUNT_LIMIT
import kotlin.math.max
import kotlin.math.min
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
    private val touchHelper: TouchHelper by lazy(LazyThreadSafetyMode.NONE) { TouchHelper() }

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
    internal val drawableStateHelper: DrawableStateHelper by lazy(LazyThreadSafetyMode.NONE) { DrawableStateHelper() }

    /**
     * Вспомогательный класс для отладки текстовой разметки.
     * Для включения отладочного мода необходимо переключить [isInspectMode] в true.
     * Может оказаться крайне полезным на этапе интеграции [TextLayout].
     */
    private val inspectHelper = if (isInspectMode) InspectHelper() else null

    /**
     * Текущая текстовая разметка.
     * Лениво инициализируется при первом обращении к [TextLayout.layout].
     */
    private var _layout: Layout? = null

    /**
     * Рисуемая текстовая разметка.
     * Устанавливается при вызове [layout], когда определяются координаты текста и самого [TextLayout].
     */
    private var drawingLayout: Layout? = null

    private var precomputedData: PrecomputedLayoutData? = null
    private var isPrecomputedActual = false
    private var refreshPrecomputedCount: Int = 0

    private var cachedBoring: BoringLayout.Metrics? = null
    private var boringLayout: BoringLayout? = null

    private var viewLayoutDirection: Int = View.LAYOUT_DIRECTION_LTR
    private var viewTextDirection: Int = View.TEXT_DIRECTION_FIRST_STRONG
    private var textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR

    /**
     * Прикешированное состояние для исключения повторных измерений при многократных обращениях.
     */
    private val state = CachedState()

    /**
     * Получить снимок внутреннего состояния.
     */
    @VisibleForTesting
    internal val stateSnapshot: StateSnapshot
        get() = StateSnapshot(
            params = params,
            layout = _layout,
            isLayoutChanged = isLayoutChanged,
            textPos = layoutRect.left to layoutRect.top,
            refreshPrecomputedCount = refreshPrecomputedCount,
            fadingEdgeRule = fadingEdgeRule,
            isFadeEdgeVisible = isFadeEdgeVisible
        )

    /**
     * Получить снимок внутреннего состояния.
     */
    @VisibleForTesting
    internal val cachedStateSnapshot: CachedStateSnapshot
        get() = CachedStateSnapshot(
            resetCount = state.resetCount,
            configuredTextCount = state.configuredTextCount,
            horizontalPaddingCount = state.horizontalPaddingCount,
            verticalPaddingCount = state.verticalPaddingCount,
            textWidthCount = state.textWidthCount,
            maxTextWidthCount = state.maxTextWidthCount,
            minTextWidthCount = state.minTextWidthCount,
            minHeightByLinesCount = state.minHeightByLinesCount,
            layoutMaxHeightCount = state.layoutMaxHeightCount,
            isVisibleCount = state.isVisibleCount,
            widthCount = state.widthCount,
            heightCount = state.heightCount
        )

    /**
     * Признак необходимости в построении layout при следующем обращении
     * по причине изменившихся данных.
     */
    private var isLayoutChanged: Boolean = true
        set(value) {
            field = value
            if (value) {
                state.reset()
                isPrecomputedActual = false
            }
        }

    /**
     * Прозрачность цвета краски текста.
     */
    private var textColorAlpha = textPaint.alpha

    private var autoSizeTextSizes = intArrayOf()
    private var autoSizesCalculated: Boolean = false
    private val autoTextSizePaint by lazy(LazyThreadSafetyMode.NONE, ::SimpleTextPaint)
    private val availableSpaceRect = Rect()

    /**
     * Правило попытки использования затемнения сокращения [requiresFadingEdge] и [fadeEdgeSize].
     * Затемнение, как и для TextView, может происходить только для [isSingleLine] и [ellipsize] == null параметров,
     * c ограничение длины в виде layoutWidth.
     */
    private val fadingEdgeRule: Boolean
        get() = requiresFadingEdge && fadeEdgeSize > 0 &&
                params.layoutWidth != null &&
                params.ellipsize == null &&
                params.isSingleLine

    /**
     * Признак о необходимости отрисовки затеменения для текста, который полностью не вмещается в разметку.
     */
    private var isFadeEdgeVisible: Boolean = false
    private val fadeMatrix by lazy(LazyThreadSafetyMode.NONE) { Matrix() }
    private val fadePaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
    }
    private var fadeShader: Lazy<Shader>? = null
    private fun createFadeShader(): Lazy<Shader> = lazy(LazyThreadSafetyMode.NONE) {
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
    private val rect = Rect()

    /**
     * Координаты границ отрисовки [layout] (не текста внутри него), формируются вместе с [rect].
     */
    private val layoutRect = RectF()
    private val translationRect = RectF()
    private val clipRect = RectF()
    private var isSimpleDrawing = true

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
     * Имеет ленивую инициализацию в случае изменения параметров.
     */
    val layout: Layout
        get() { // TODO не очевидно, что метод может внутри что-то пересчитывать.
            // TODO предлагаю дать доступ к _layout как optional и сделать отдельный метод типа requireLayout()
            // TODO т.к не во всех случаях требуется пересчет, иногда нужно просто посмотреть что лежит в layout
            val lastLayout = _layout
            return if (!isLayoutChanged && lastLayout != null) {
                lastLayout
            } else {
                val precomputedData = state.getLayoutPrecomputedData()
                createLayoutInternal(precomputedData).also { layout ->
                    isLayoutChanged = false
                    _layout = layout
                    if (layout is BoringLayout) boringLayout = layout

                    updateFadeEdgeVisibility()
                }
            }
        }

    /**
     * Получить [RectF] границ внутреннего [layout].
     */
    val innerLayoutRect: RectF
        get() = RectF(layoutRect)

    /**
     * Краска текста разметки.
     */
    val textPaint: TextPaint
        get() = params.paint

    /**
     * Видимость разметки.
     */
    val isVisible: Boolean
        get() = state.isVisible

    /**
     * Максимальное количество строк ограничено одной.
     * Также принудительно будет создаваться только [BoringLayout] для [String].
     */
    val isSingleLine: Boolean
        get() = params.isSingleLine

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
     * Максимальная ширина разметки.
     */
    val maxWidth: Int?
        get() = params.maxWidth

    /**
     * Минимальная ширина разметки.
     */
    val minWidth: Int
        get() = params.minWidth

    /**
     * Максимальная высота разметки.
     */
    val maxHeight: Int?
        get() = params.maxHeight

    /**
     * Минимальная высота разметки.
     */
    val minHeight: Int
        get() = params.minHeight

    /**
     * Максимальное количество символов в строке.
     */
    val maxLength: Int
        get() = params.maxLength

    /**
     * Массив отступов строк слева в пикселях. Индекс в массиве соответствует номеру строки.
     */
    val leftIndents: IntArray?
        get() = params.indents?.left

    /**
     * Массив отступов строк справа в пикселях. Индекс в массиве соответствует номеру строки.
     */
    val rightIndents: IntArray?
        get() = params.indents?.right

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
     * Горизонтальные внутренние отступы.
     * [paddingStart] + [paddingEnd].
     */
    val horizontalPadding: Int
        get() = state.horizontalPadding

    /**
     * Горизонтальные внутренние отступы.
     * [paddingTop] + [paddingBottom].
     */
    val verticalPadding: Int
        get() = state.verticalPadding

    /**
     * Ширина всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val width: Int
        get() = state.width

    /**
     * Высота всей разметки.
     *
     * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     */
    @get:Px
    val height: Int
        get() = state.height

    /**
     * Базовая линия текстовой разметки.
     */
    @get:Px
    val baseline: Int
        get() = paddingTop + layout.getLineBaseline(0)

    /**
     * Базовая линия текстовой разметки, вызов которой не вызывает лишних построений [layout].
     */
    @get:Px
    val safeLayoutBaseLine: Int
        get() = if (_layout == null) {
            -1
        } else {
            paddingTop + layout.getLineBaseline(0)
        }

    var layoutFactory: LayoutFactory = LayoutCreator

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
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) updateCanvasRects()
        }

    /**
     * Смещение отрисовки разметки по оси X.
     */
    var translationX: Float = 0f
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) updateCanvasRects()
        }

    /**
     * Смещение отрисовки разметки по оси Y.
     */
    var translationY: Float = 0f
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) updateCanvasRects()
        }

    /**
     * Признак необходимости показа затемнения текста при сокращении.
     */
    var requiresFadingEdge: Boolean = false

    /**
     * Ширина затенения текста, если он не помещается в разметку.
     */
    var fadeEdgeSize: Int = 0
        set(value) {
            val safeValue = value.coerceAtLeast(0)
            val isChanged = field != safeValue
            field = safeValue
            if (isChanged) {
                updateCanvasRects()
                fadeShader = createFadeShader()
            }
        }

    /**
     * Установить режим автоматического определения размера текста.
     * Размер текста исходит из заданных параметров:
     * [autoSizeMaxTextSize] - Максимального размера текста.
     * [autoSizeMinTextSize] - Минимального размера текста.
     * [autoSizeStepGranularity] - Шаг перебора интервала от минимального до максимального размера.
     *
     * Для безусловного расчета размера текста для всего доступного пространства, даже если текст влезает -
     * необходимо установить [isAutoSizeForAvailableSpace].
     * (Чтобы добиться поведения аналогичного TextView с конкретными width и height, когда задан autoSizeTextType)
     * Для ограничения максимальной высоты [TextLayout] необходимо использовать [autoSizeAvailableHeight]
     * или [TextLayoutParams.maxHeight].
     */
    var isAutoTextSizeMode: Boolean = false

    /**
     * Установить состояние необходимости использовать все доступное пространство для определения размера текста,
     * даже есть он помещается в выделенное пространство с текущим размером текста.
     * Используется при включении [isAutoTextSizeMode].
     */
    var isAutoSizeForAvailableSpace: Boolean = false

    /**
     * Установить максимально допустимую высоту для режима автоматического определения размера текста.
     *
     * Использовать, когда при включении режима [isAutoTextSizeMode] необходимо ограничение по высоте
     * для автоопределения размера текста и явно не указана максимальная высота разметки [TextLayoutParams.maxHeight].
     */
    @Px
    var autoSizeAvailableHeight: Int = Int.MAX_VALUE

    /**
     * Установить максимальный размер текста для режима автоматического определения размера текста [isAutoTextSizeMode].
     */
    @Px
    var autoSizeMaxTextSize: Int = DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE
        set(value) {
            if (field != value) autoSizesCalculated = false
            field = value
        }

    /**
     * Установить минимальный размер текста для режима автоматического определения размера текста [isAutoTextSizeMode].
     */
    @Px
    var autoSizeMinTextSize: Int = DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE
        set(value) {
            if (field != value) autoSizesCalculated = false
            field = value
        }

    /**
     * Установить шаг перебора интервала от минимального до максимального размера текста
     * для режима автоматического определения размера текста [isAutoTextSizeMode].
     */
    @Px
    var autoSizeStepGranularity: Int = DEFAULT_AUTO_SIZE_STEP_GRANULARITY
        set(value) {
            if (field != value) autoSizesCalculated = false
            field = value
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
     * Установить/получить состояние активации текстовой разметки.
     *
     * @see colorStateList
     */
    var isActivated: Boolean = false
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) drawableStateHelper.setActivated(value)
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
     * @see [TextLayoutParams.highlights]
     */
    val highlights: TextHighlights?
        get() = params.highlights

    /**
     * Цвет тени текста.
     * @see setShadowLayer
     */
    var shadowColor: Int = Color.BLACK
        private set

    /**
     * Радиус размытия тени текста.
     * @see setShadowLayer
     */
    var shadowRadius: Float = 0f
        private set

    /**
     * Смещение по X тени текста.
     * @see setShadowLayer
     */
    var shadowDx: Float = 0f
        private set

    /**
     * Смещение по Y тени текста.
     * @see setShadowLayer
     */
    var shadowDy: Float = 0f
        private set

    /**
     * Получить ожидаемую ширину разметки для однострочного текста [text] без создания [StaticLayout].
     * По-умолчанию используется текст из параметров рамзетки [TextLayoutParams.text].
     */
    @Px
    fun getDesiredWidth(text: CharSequence? = null): Int {
        val resultText = text ?: params.text

        val textWidth = if (resultText is Spanned) {
            with(resultText) {
                // Можно доработать для RelativeSizeSpan и когда текст состоит из спанов частично
                val sizeSpans = getSpans(0, length, AbsoluteSizeSpan::class.java)
                if (sizeSpans.isEmpty()) {
                    params.paint.getTextWidth(resultText)
                } else {
                    var spansWidth = 0
                    sizeSpans.forEach {
                        val start = getSpanStart(it)
                        val end = getSpanEnd(it)
                        spansWidth += TextPaint(params.paint)
                            .apply {
                                textSize = it.size.toFloat()
                            }
                            .getTextWidth(this, start, end)
                    }
                    spansWidth
                }
            }
        } else {
            params.paint.getTextWidth(resultText)
        }
        return horizontalPadding + textWidth
    }

    /**
     * Получить ожидаемую высоту разметки для однострочного текста без создания [StaticLayout].
     * Для [Spanned] возвращается максимальная высота по каждому [AbsoluteSizeSpan].
     */
    @Px
    fun getDesiredHeight(): Int {
        val text = params.text
        val textHeight = if (text is Spanned) {
            val spans = text.getSpans(0, text.length, AbsoluteSizeSpan::class.java)
            if (spans.isEmpty()) {
                getTextHeight()
            } else {
                var maxHeight = 0
                spans.forEach { span ->
                    val sizedTextPaint = TextPaint(textPaint).apply { textSize = span.size.toFloat() }
                    val height = getTextHeight(sizedTextPaint)
                    maxHeight = maxOf(maxHeight, height)
                }
                maxHeight
            }
        } else {
            getTextHeight()
        }
        return verticalPadding + textHeight
    }

    private fun getTextHeight(paint: TextPaint = textPaint): Int =
        paint.fontMetrics.let {
            (it.bottom - it.top + it.leading).roundToInt()
        }

    /**
     * Получить преподсчитанную ширину [TextLayout] с учетом всех текущих параметров [params]
     * и свободного пространства [availableWidth], которое ограничивает возможную ширину [TextLayout]
     * по верхней границе.
     *
     * Если [availableWidth] - null, то метод не накладывает дополнительных органичений
     * на ширину [TextLayout] при вычислении.
     *
     * @return ширину [TextLayout] с учетом текущих [params] и максимального ограничения [availableWidth].
     * Возвращаемое значение ключает в себя горизонтальыне отступы [TextLayout.horizontalPadding].
     */
    @Px
    fun getPrecomputedWidth(availableWidth: Int? = null): Int =
        state.getPrecomputedWidth(availableWidth)

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
     * @param checkDiffs true, если необходимо принудительно проверить наличие изменений,
     * в ином случае проверки будут игнорироваться, если параметры уже находятся в измененном состоянии.
     * @return true, если параметры изменились.
     */
    fun configure(
        checkDiffs: Boolean = false,
        config: TextLayoutConfig
    ): Boolean =
        if (!checkDiffs && isLayoutChanged) {
            config.invoke(params)
            isLayoutChanged = true
            true
        } else {
            checkDiff(config).also { isChanged ->
                if (isChanged) isLayoutChanged = true
            }
        }

    /**
     * Настроить разметку.
     * @see configure
     * Необходим для поддержки сценариев передачи TextLayoutConfig в качестве объекта, а не лямбды.
     */
    fun configure(config: TextLayoutConfig): Boolean =
        configure(checkDiffs = false, config)

    private fun checkDiff(config: TextLayoutConfig): Boolean {
        val oldTextSize = params.paint.textSize
        val oldLetterSpacing = params.paint.letterSpacing
        val oldTypeface = params.paint.typeface
        val oldColor = params.paint.color
        val oldParams = params.copy()

        config.invoke(params)

        if (oldColor != params.paint.color) {
            textColorAlpha = params.paint.alpha
            params.paint.alpha = (textColorAlpha * alpha).toInt()
        }

        val isTextSizeChanged =
            oldTextSize != params.paint.textSize ||
                    oldLetterSpacing != params.paint.letterSpacing ||
                    oldTypeface != params.paint.typeface

        return isTextSizeChanged || oldParams != params // TODO унести весь код сравнения в кастомный equals TextLayoutParams
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
        } else {
            isLayoutChanged
        }
        if (isVisible) layout
        return isChanged
    }

    /**
     * Построить разметку с шириной [width].
     * Метод является оптимизированным решением для явной попытки построения [layout] с шириной [width]
     * без участия [configure], чтобы сохранить ранее вычисленные данные из [getPrecomputedWidth]
     * при последующей установке ширины из этого результата.
     */
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
     * Метод вызывает построение [Layout], если ранее он еще не был создан,
     * или если [params] разметки были изменены путем вызова [configure],
     * в иных случаях лишнего построения не произойдет.
     *
     * Актуальный построенный [Layout] будет установлен в [drawingLayout],
     * отрисовка которого происходит в [drawLayout].
     */
    fun layout(@Px left: Int, @Px top: Int) {
        rect.set(left, top, left + width, top + height)
        val topOffset = getTextTopOffset().toFloat()
        layoutRect.set(
            this.left + paddingStart.toFloat(),
            topOffset,
            this.right - paddingEnd.toFloat(),
            topOffset + state.minHeightByLines - verticalPadding
        )
        if (!isSimpleDrawing) updateCanvasRects()

        drawingLayout = _layout

        touchHelper.updateTouchRect()
        inspectHelper?.updatePositions()
    }

    private fun updateCanvasRects() {
        isSimpleDrawing = false
        translationRect.set(layoutRect)
        translationRect.offset(translationX, translationY)
        if (shadowRadius != 0f) {
            clipRect.set(
                translationRect.left + min(0f, shadowDx - shadowRadius),
                translationRect.top + min(0f, shadowDy - shadowRadius),
                translationRect.right + max(0f, shadowDx + shadowRadius),
                translationRect.bottom + max(0f, shadowDy + shadowRadius)
            )
        } else {
            clipRect.set(translationRect)
        }
    }

    /**
     * Нарисовать разметку.
     *
     * Рисуется именно кэш текстовой разметки [drawingLayout],
     * чтобы не допускать построения layout на [View.onDraw].
     */
    fun draw(canvas: Canvas) {
        drawingLayout?.let { layout ->
            if (!isVisible || layout.text.isEmpty()) return
            if (isSimpleDrawing) {
                drawSimpleLayout(canvas, layout)
            } else {
                drawLayout(canvas, layout)
            }
        }
    }

    private fun getTextTopOffset(): Int {
        val layoutHeight = state.minHeightByLines
        val gravity = params.verticalGravity and Gravity.VERTICAL_GRAVITY_MASK
        return when {
            layoutHeight >= height -> {
                top + paddingTop
            }
            gravity == Gravity.BOTTOM -> {
                bottom - paddingBottom - layoutHeight
            }
            gravity == Gravity.CENTER || gravity == Gravity.CENTER_VERTICAL -> {
                top + paddingTop + (height - paddingTop - paddingBottom - layoutHeight) / 2
            }
            else -> {
                top + paddingTop
            }
        }
    }

    private fun drawSimpleLayout(canvas: Canvas, layout: Layout) {
        inspectHelper?.draw(canvas)
        canvas.withClip(
            left = layoutRect.left,
            top = layoutRect.top,
            right = layoutRect.right,
            bottom = layoutRect.bottom
        ) {
            withTranslation(
                x = layoutRect.left,
                y = layoutRect.top
            ) {
                layout.draw(this)
            }
        }
    }

    private fun drawLayout(canvas: Canvas, layout: Layout) {
        canvas.withRotation(
            degrees = rotation,
            pivotX = left + rect.width() / 2f,
            pivotY = top + rect.height() / 2f
        ) {
            inspectHelper?.draw(this)
            withFade {
                withClip(
                    left = clipRect.left,
                    top = clipRect.top,
                    right = clipRect.right,
                    bottom = clipRect.bottom
                ) {
                    withTranslation(
                        x = translationRect.left,
                        y = translationRect.top
                    ) {
                        layout.draw(this)
                    }
                }
            }
        }
    }

    private fun Canvas.withFade(function: (Canvas) -> Unit) {
        if (isFadeEdgeVisible) {
            val saveCount = saveLayer(0f, 0f, right.toFloat(), bottom.toFloat(), null)
            val fadeLeft = right.toFloat() - fadeEdgeSize
            function(this)
            fadeMatrix.reset()
            fadeMatrix.postTranslate(fadeLeft, 0f)
            fadeShader?.value?.setLocalMatrix(fadeMatrix)
            drawRect(
                fadeLeft,
                top.toFloat(),
                right.toFloat(),
                bottom.toFloat(),
                fadePaint
            )
            restoreToCount(saveCount)
        } else {
            function(this)
        }
    }

    /**
     * Обработать событие изменения RTL свойств view компонента.
     *
     * Необходимо вызовать из [View.onRtlPropertiesChanged],
     * передавая [View.getLayoutDirection] и [View.getTextDirection].
     */
    fun onRtlPropertiesChanged(layoutDirection: Int, textDirection: Int) {
        if (viewLayoutDirection != layoutDirection || viewTextDirection != textDirection) {
            viewLayoutDirection = layoutDirection
            viewTextDirection = textDirection
            textDir = getTextDirectionHeuristic()
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
        if (maxLines == SINGLE_LINE || isSingleLine) {
            val ellipsisStart = layout.getEllipsisStart(0)
            val layoutTextLength = if (ellipsisStart > 0) {
                ellipsisStart - 1
            } else {
                layout.text.count()
            }
            params.text.count() - layoutTextLength
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
     * Установить для текста тень указанного радиуса размытия [radius] и цвета [color],
     * а также смещения по [dx] и [dy] от положения текста.
     */
    fun setShadowLayer(radius: Float, dx: Float, dy: Float, color: Int) {
        textPaint.setShadowLayer(radius, dx, dy, color)
        shadowRadius = radius
        shadowDx = dx
        shadowDy = dy
        shadowColor = color
        updateCanvasRects()
    }

    private fun createLayoutInternal(precomputedData: PrecomputedLayoutData): Layout =
        createLayout(precomputedData)
            .takeIf { !isAutoSizeRequired(it, precomputedData) }
            ?: getAutoSizedLayout(precomputedData)

    /**
     * Обновить разметку по набору параметров [params] и [precomputedData].
     */
    private fun createLayout(
        precomputedData: PrecomputedLayoutData,
        paint: TextPaint = params.paint,
        width: Int = state.textWidth,
        ellipsize: TruncateAt? = params.ellipsize
    ): Layout =
        LayoutConfigurator.createLayout(
            text = state.configuredText,
            paint = paint,
            factory = layoutFactory
        ) {
            this.width = width
            this.ellipsize = ellipsize
            maxHeight = state.layoutMaxHeight
            alignment = params.alignment
            includeFontPad = params.includeFontPad
            spacingAdd = params.spacingAdd
            spacingMulti = params.spacingMulti
            maxLines = params.maxLines
            isSingleLine = params.isSingleLine
            highlights = params.highlights
            breakStrategy = params.breakStrategy
            hyphenationFrequency = params.hyphenationFrequency
            fadingEdgeSize = if (fadingEdgeRule) fadeEdgeSize else 0
            textDir = this@TextLayout.textDir
            hasAffectingSymbols = precomputedData.hasAffectingSymbols
            lineLastSymbolIndex = precomputedData.lineLastSymbolIndex
            boring = precomputedData.isBoring
            boringLayout = this@TextLayout.boringLayout
            indents = params.indents
        }

    private fun isAutoSizeRequired(layout: Layout, precomputedData: PrecomputedLayoutData): Boolean {
        val needCheckAutoSize = isAutoTextSizeMode && layout.text.isNotEmpty() && layout.width > 0
        return if (needCheckAutoSize) {
            updateAvailableTextSpaceRect(precomputedData)
            isAutoSizeForAvailableSpace ||
                    layout.getEllipsisCount(layout.lineCount - 1) > 0 ||
                    layout.height > availableSpaceRect.height()
        } else {
            false
        }
    }

    private fun getAutoSizedLayout(precomputedData: PrecomputedLayoutData): Layout {
        setupAutoSizeText()
        return findLargestFitsLayout(precomputedData)
    }

    private fun setupAutoSizeText() {
        autoTextSizePaint.apply {
            reset()
            set(textPaint)
            textSize = textPaint.textSize
        }

        if (autoSizesCalculated) return

        val autoSizeValuesLength = (autoSizeMaxTextSize - autoSizeMinTextSize) / autoSizeStepGranularity
        autoSizeTextSizes = IntArray(autoSizeValuesLength)
        var sizeValue = autoSizeMinTextSize
        repeat(autoSizeValuesLength) { index ->
            autoSizeTextSizes[index] = sizeValue
            sizeValue += autoSizeStepGranularity
        }

        autoSizesCalculated = true
    }

    private fun findLargestFitsLayout(precomputedData: PrecomputedLayoutData): Layout {
        updateAvailableTextSpaceRect(precomputedData)
        val autoPrecomputedData = precomputedData.copy(
            lineLastSymbolIndex = null,
            isBoring = null
        )

        val sizesCount = autoSizeTextSizes.size
        var bestSizeIndex = 0
        var lowIndex = 1
        var highIndex = sizesCount - 1
        var sizeToTryIndex: Int
        while (lowIndex <= highIndex) {
            sizeToTryIndex = (lowIndex + highIndex) / 2
            if (suggestedSizeFits(autoSizeTextSizes[sizeToTryIndex], autoPrecomputedData)) {
                bestSizeIndex = lowIndex
                lowIndex = sizeToTryIndex + 1
            } else {
                highIndex = sizeToTryIndex - 1
                bestSizeIndex = highIndex
            }
        }
        val optimalTextSize = autoSizeTextSizes[bestSizeIndex]
        autoTextSizePaint.textSize = optimalTextSize.toFloat()

        return createLayout(autoPrecomputedData, autoTextSizePaint, availableSpaceRect.width())
    }

    private fun updateAvailableTextSpaceRect(precomputedData: PrecomputedLayoutData) {
        val availableHeight = minOf(params.maxHeight ?: Int.MAX_VALUE, autoSizeAvailableHeight)
        val maxTextHeight = (availableHeight - state.verticalPadding).coerceAtLeast(0)
        val widthLimit = params.layoutWidth
            ?: minOf(
                precomputedData.availableWidth ?: Int.MAX_VALUE,
                params.maxWidth ?: Int.MAX_VALUE
            )
        val textLimit = widthLimit - state.horizontalPadding

        availableSpaceRect.set(0, 0, textLimit, maxTextHeight)
    }

    private fun suggestedSizeFits(
        suggestedSizeInPx: Int,
        precomputedData: PrecomputedLayoutData
    ): Boolean {
        autoTextSizePaint.textSize = suggestedSizeInPx.toFloat()
        val layout = createLayout(
            precomputedData = precomputedData,
            paint = autoTextSizePaint,
            width = availableSpaceRect.width(),
            ellipsize = TruncateAt.END
        )
        return layout.getEllipsisCount(layout.lineCount - 1) <= 0 && layout.height <= availableSpaceRect.height()
    }

    /**
     * Обновить признак [isFadeEdgeVisible] о необходимости отрисовки затеменения для текста,
     * который полностью не вмещается в разметку.
     */
    private fun updateFadeEdgeVisibility() {
        isFadeEdgeVisible = fadingEdgeRule && state.textWidth < layout.width
    }

    private fun getTextDirectionHeuristic(): TextDirectionHeuristic {
        val defaultIsRtl = viewLayoutDirection == View.LAYOUT_DIRECTION_RTL
        val defaultHeuristics = if (defaultIsRtl) {
            TextDirectionHeuristics.FIRSTSTRONG_RTL
        } else {
            TextDirectionHeuristics.FIRSTSTRONG_LTR
        }
        return when (viewTextDirection) {
            View.TEXT_DIRECTION_FIRST_STRONG -> defaultHeuristics
            View.TEXT_DIRECTION_ANY_RTL -> TextDirectionHeuristics.ANYRTL_LTR
            View.TEXT_DIRECTION_LTR -> TextDirectionHeuristics.LTR
            View.TEXT_DIRECTION_RTL -> TextDirectionHeuristics.RTL
            View.TEXT_DIRECTION_LOCALE -> TextDirectionHeuristics.LOCALE
            View.TEXT_DIRECTION_FIRST_STRONG_LTR -> TextDirectionHeuristics.FIRSTSTRONG_LTR
            View.TEXT_DIRECTION_FIRST_STRONG_RTL -> TextDirectionHeuristics.FIRSTSTRONG_RTL
            else -> defaultHeuristics
        }
    }

    /**
     * Параметры для создания текстовой разметки [Layout] в [TextLayout].
     *
     * @property text текста разметки.
     * @property paint краска текста.
     * @property layoutWidth ширина разметки. Null -> WRAP_CONTENT.
     * @property alignment мод выравнивания текста.
     * @property verticalGravity вертикальная гравитация текста относительно высоты [height].
     * @property ellipsize мод сокращения текста.
     * @property includeFontPad включить стандартные отступы шрифта.
     * @property spacingAdd величина межстрочного интервала.
     * @property spacingMulti множитель межстрочного интервала.
     * @property maxLines максимальное количество строк.
     * @property minLines минимальное количество строк.
     * @property isSingleLine true, если необходимо создать именно [BoringLayout].
     * Логика кажется странной, но у [TextView] на singleLine безусловно создается [BoringLayout],
     * а с [maxLines] == 1 только в случае, когда ширина текста меньше [width].
     * При этом есть отличие отображения [BoringLayout] с [ellipsize] null будет обрезать текст,
     * а [StaticLayout] будет обрезаться до последнего помещающегося символа.
     * @property maxLength максимальное количество символов в строке.
     * @property isVisible состояние видимости разметки.
     * @property isVisibleWhenBlank мод скрытия разметки при пустом тексте, включая [padding].
     * и скорость создания [StaticLayout]. (Использовать только для [maxLines] > 1, когда текст может содержать ссылки).
     * @property padding внутренние отступы разметки.
     * @property highlights модель для выделения текста.
     * @property minWidth минимальная ширина разметки.
     * @property minHeight минимальная высота разметки.
     * @property maxWidth максимальная ширина разметки.
     * @property maxHeight максимальная высота разметки с учетом [padding]. Необходима для автоматического подсчета [maxLines].
     * @property breakStrategy стратегия разрыва строки, см [Layout.BREAK_STRATEGY_SIMPLE].
     * @property hyphenationFrequency частота переноса строк, см. [Layout.HYPHENATION_FREQUENCY_NONE].
     * @property needHighWidthAccuracy true, если необходимо включить мод высокой точности ширины текста.
     * Механика релевантна для однострочных разметок с сокращением текста, к размерам которых привязаны другие элементы.
     * После сокращения текста [StaticLayout] не всегда имеет точные размеры строго по границам текста ->
     * иногда остается лишнее пространство, которое может оказаться критичным для отображения.
     * [needHighWidthAccuracy] решает эту проблему, но накладывает дополнительные расходы на вычисления при перестроении разметки.
     * @property indents отступы слева/справа в пикселях для любой строки разметки.
     */
    data class TextLayoutParams(
        var text: CharSequence = StringUtils.EMPTY,
        var paint: TextPaint = SimpleTextPaint(),
        @Px var layoutWidth: Int? = null,
        var alignment: Alignment = Alignment.ALIGN_NORMAL,
        var verticalGravity: Int = Gravity.NO_GRAVITY,
        var ellipsize: TruncateAt? = TruncateAt.END,
        var includeFontPad: Boolean = true,
        var spacingAdd: Float = DEFAULT_SPACING_ADD,
        var spacingMulti: Float = DEFAULT_SPACING_MULTI,
        var maxLines: Int = SINGLE_LINE,
        var isSingleLine: Boolean = false,
        var minLines: Int = 0,
        var maxLength: Int = Int.MAX_VALUE,
        var isVisible: Boolean = true,
        var isVisibleWhenBlank: Boolean = true,
        var padding: TextLayoutPadding = TextLayoutPadding(),
        var highlights: TextHighlights? = null,
        @Px var minWidth: Int = 0,
        @Px var minHeight: Int = 0,
        @Px var maxWidth: Int? = null,
        @Px var maxHeight: Int? = null,
        var breakStrategy: Int = 0,
        var hyphenationFrequency: Int = 0,
        var needHighWidthAccuracy: Boolean = false,
        var indents: TextLineIndents? = null
    ) {

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
    ) {
        constructor(@Px padding: Int) : this(padding, padding, padding, padding)
    }

    /**
     * Настройки отступов для каждой строки [StaticLayout].
     * Индекс в массиве соответствует номеру строки.
     *
     * @see StaticLayout.Builder.setIndents
     */
    data class TextLineIndents(
        var left: IntArray? = null,
        var right: IntArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TextLineIndents

            if (left != null) {
                if (other.left == null) return false
                if (!left.contentEquals(other.left)) return false
            } else if (other.left != null) return false
            if (right != null) {
                if (other.right == null) return false
                if (!right.contentEquals(other.right)) return false
            } else if (other.right != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = left?.contentHashCode() ?: 0
            result = 31 * result + (right?.contentHashCode() ?: 0)
            return result
        }
    }

    /**
     * Вспомогатльная реализация для кэширования измерений в текущем состоянии [TextLayout].
     * Не использовать lazy и пересоздание этого объекта, влияет на производительность, из-за чего
     * не будет пользы в прикэшировании.
     */
    private inner class CachedState {

        private var _configuredText: CharSequence? = null
        private var _horizontalPadding: Int? = null
        private var _verticalPadding: Int? = null
        private var _textWidth: Int? = null
        private var _maxTextWidth: Int? = null
        private var _minTextWidth: Int? = null
        private var _limitedTextWidth: Int? = null
        private var _minHeightByLines: Int? = null
        private var _layoutMaxHeight: Int? = null
        private var _isVisible: Boolean? = null
        private var _width: Int? = null
        private var _height: Int? = null

        var resetCount: Int = 0
        var configuredTextCount: Int = 0
        var horizontalPaddingCount: Int = 0
        var verticalPaddingCount: Int = 0
        var textWidthCount: Int = 0
        var maxTextWidthCount: Int = 0
        var minTextWidthCount: Int = 0
        var minHeightByLinesCount: Int = 0
        var layoutMaxHeightCount: Int = 0
        var isVisibleCount: Int = 0
        var widthCount: Int = 0
        var heightCount: Int = 0

        /**
         * Сбросить сохранненные вычисления.
         */
        fun reset() {
            _configuredText = null
            _horizontalPadding = null
            _verticalPadding = null
            _textWidth = null
            _maxTextWidth = null
            _minTextWidth = null
            _limitedTextWidth = null
            _minHeightByLines = null
            _layoutMaxHeight = null
            _isVisible = null
            _width = null
            _height = null
            resetCount += 1
        }

        /**
         * Сконфигурированный текст с учетом настроек параметров.
         */
        val configuredText: CharSequence
            get() = _configuredText ?: with(params) {
                when {
                    maxLength == Int.MAX_VALUE || maxLength < 0 -> text
                    text.isEmpty() -> text
                    maxLength >= text.length -> text
                    else -> text.subSequence(0, maxLength)
                }.also {
                    _configuredText = it
                    configuredTextCount += 1
                }
            }

        /**
         * Горизонтальные отступы.
         * Сумма левого и правого отступа от границ [TextLayout] до [layout].
         */
        val horizontalPadding: Int
            get() = _horizontalPadding
                ?: (params.padding.start + params.padding.end)
                    .also {
                        _horizontalPadding = it
                        horizontalPaddingCount += 1
                    }

        /**
         * Вертикальные отступы.
         * Сумма верхнего и нижнего отступа от границ [TextLayout] до [layout].
         */
        val verticalPadding: Int
            get() = _verticalPadding
                ?: (params.padding.top + params.padding.bottom)
                    .also {
                        _verticalPadding = it
                        verticalPaddingCount += 1
                    }

        /**
         * Ширина текста.
         */
        @get:Px
        val textWidth: Int
            get() = _textWidth
                ?: with(params) {
                    layoutWidth?.let { width ->
                        maxOf(width - horizontalPadding, 0)
                    } ?: getLayoutPrecomputedData().precomputedTextWidth
                }.also {
                    _textWidth = it
                    textWidthCount += 1
                }

        /**
         * Максимальная ширина текста.
         */
        @get:Px
        val maxTextWidth: Int
            get() = _maxTextWidth
                ?: with(params) {
                    maxWidth?.let { maxOf(it - horizontalPadding, 0) } ?: Integer.MAX_VALUE
                }.also {
                    _maxTextWidth = it
                    maxTextWidthCount += 1
                }

        /**
         * Минимальная ширина текста.
         */
        @get:Px
        val minTextWidth: Int
            get() = _minTextWidth
                ?: with(params) {
                    if (minWidth > 0) maxOf(minWidth - horizontalPadding, 0) else 0
                }.also {
                    _minTextWidth = it
                    minTextWidthCount += 1
                }

        /**
         * Минимальная высота текста по заданным [TextLayoutParams.minLines].
         */
        @get:Px
        val minHeightByLines: Int
            get() = if (_minHeightByLines != null) {
                _minHeightByLines!!
            } else {
                when {
                    params.maxLines <= 0 || !isVisible -> 0
                    params.isSingleLine && layout.lineCount > 0 -> layout.getLineTop(1)
                    params.maxLines <= layout.lineCount -> layout.getLineTop(params.maxLines)
                    params.minLines <= layout.lineCount -> layout.height
                    else -> {
                        val lineHeight = with(params) {
                            (paint.getFontMetricsInt(null) * spacingMulti + spacingAdd).roundToInt()
                        }
                        layout.height + (params.minLines - layout.lineCount) * lineHeight
                    }
                } + verticalPadding
            }.also {
                _minHeightByLines = it
                minHeightByLinesCount += 1
            }

        /**
         * Максимальная высота текста.
         */
        @get:Px
        val layoutMaxHeight: Int?
            get() = _layoutMaxHeight
                ?: with(params) {
                    maxHeight?.let { maxOf(it - verticalPadding, 0) }
                }.also {
                    _layoutMaxHeight = it
                    layoutMaxHeightCount += 1
                }

        /**
         * Видимость разметки.
         */
        val isVisible: Boolean
            get() = _isVisible
                ?: with(params) {
                    if (!isVisibleWhenBlank) {
                        isVisible && text.isNotBlank()
                    } else {
                        isVisible
                    }
                }.also {
                    _isVisible = it
                    isVisibleCount += 1
                }

        /**
         * Ширина всей разметки.
         *
         * Обращение к полю вызывает построение [StaticLayout], если ранее он еще не был создан,
         * или если [params] разметки были изменены путем вызова [configure],
         * в иных случаях лишнего построения не произойдет.
         */
        @get:Px
        val width: Int
            get() = _width
                ?: when {
                    !isVisible -> 0
                    params.layoutWidth != null -> params.layoutWidth!!
                    else -> {
                        val layoutWidth = if (layout.lineCount == SINGLE_LINE && params.needHighWidthAccuracy) {
                            layout.getLineWidth(0).roundToInt()
                        } else {
                            layout.width
                        }
                        (layoutWidth + horizontalPadding)
                            .coerceAtMost(params.maxWidth ?: Int.MAX_VALUE)
                            .coerceAtLeast(params.minWidth)
                    }
                }.also {
                    _width = it
                    widthCount += 1
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
            get() = _height
                ?: if (isVisible) {
                    params.minHeight
                        .coerceAtLeast(minHeightByLines)
                        .coerceAtMost(params.maxHeight ?: Int.MAX_VALUE)
                } else {
                    0
                }.also {
                    _height = it
                    heightCount += 1
                }

        /**
         * Получить преподсчитанную ширину [TextLayout] с учетом всех текущих параметров [params]
         * и свободного пространства [availableWidth], которое ограничивает возможную ширину [TextLayout]
         * по верхней границе.
         *
         * Если [availableWidth] - null, то метод не накладывает дополнительных органичений
         * на ширину [TextLayout] при вычислении.
         *
         * @return ширину [TextLayout] с учетом текущих [params] и максимального ограничения [availableWidth].
         * Возвращаемое значение ключает в себя горизонтальыне отступы [TextLayout.horizontalPadding].
         */
        @Px
        fun getPrecomputedWidth(availableWidth: Int? = null): Int {
            val lastPrecomputedData = precomputedData
            val textWidth = when {
                text.isEmpty() && minWidth == 0 && (maxWidth ?: Int.MAX_VALUE) == Int.MAX_VALUE -> 0
                isPrecomputedActual &&
                        lastPrecomputedData != null &&
                        lastPrecomputedData.availableWidth == availableWidth -> lastPrecomputedData.precomputedTextWidth
                else -> refreshPrecomputedData(availableWidth).precomputedTextWidth
            }
            return textWidth + horizontalPadding
        }

        /**
         * Получить подготовленные данные для построения [Layout].
         *
         * Метод проверяет консистентность текущих параметров с ранее вычисленными,
         * если они совпадают - возвращает текущие данные [precomputedData],
         * если нет - обновляет [refreshPrecomputedData] и возвращает новые данные.
         */
        fun getLayoutPrecomputedData(): PrecomputedLayoutData =
            precomputedData?.takeIf {
                val layoutWidth = params.layoutWidth
                isPrecomputedActual &&
                        // Вычисляемая ширина текста для свободного пространста была применена к параметру layoutWidth.
                        (
                                (layoutWidth != null && it.precomputedTextWidth == layoutWidth - horizontalPadding) ||
                                        // Параметр layoutWidth не задавался, и вычисление ширины текста было без ограничений.
                                        (it.availableWidth == null && layoutWidth == null)
                                )
            } ?: refreshPrecomputedData(params.layoutWidth)

        private fun refreshPrecomputedData(availableWidth: Int? = null): PrecomputedLayoutData {
            val text = configuredText
            val availableTextWidth = (availableWidth ?: Int.MAX_VALUE) - horizontalPadding
            val limitedTextWidth = minOf(availableTextWidth, maxTextWidth)

            val spanLimit = text.length + 1
            val byLayout = (text is Spannable &&
                    text.nextSpanTransition(0, spanLimit, MetricAffectingSpan::class.java) != spanLimit) ||
                    text.contains("\n")

            var isBoring: BoringLayout.Metrics? = null
            var lineLastSymbolIndex: Int? = null

            if (layoutFactory == LayoutCreator) { // TODO refactor (для прикладной фабрики не нужен расчет isBoring)
                // TODO можно вынести расчет isBoring в лямбду и передавать эту лямбду в фабрику, если кому-то необходимо - вызовет по месту
                // TODO лямбда может лежать внутри TextLayout и кешировать прошлые ее вызовы, отдавая результат сразу же при тех же значениях.
                // TODO за счет этого из LayoutFactory можно будет убрать поля boring: BoringLayout.Metrics?, boringLayout: BoringLayout? и
                // TODO вместо них добавить какой-нибудь интерфейс layoutDetector: LayoutDetector { fun isBoring(...) }
                // TODO так же убрать логику checkBoring из класса LayoutCreator, использовать там layoutDetector
                if (!byLayout &&
                    !textDir.isRtl(text, 0, text.length.coerceAtMost(RTL_SYMBOLS_CHECK_COUNT_LIMIT)) &&
                    (params.isSingleLine || text.isEmpty() ||
                            (text !is Spannable &&
                                    (text.length <= BORING_LAYOUT_TEXT_LENGTH_LIMIT || params.maxLines == Int.MAX_VALUE))
                            ) || fadingEdgeRule
                ) {
                    isBoring = BoringLayout.isBoring(text, params.paint, cachedBoring)
                }
            }
            val precomputedTextWidth = when {
                isBoring != null -> {
                    cachedBoring = isBoring
                    maxOf(minOf(isBoring.width, limitedTextWidth), minTextWidth)
                }
                availableWidth != null || params.maxWidth != null -> {
                    val (width, lastIndex) = params.paint.getTextWidth( // TODO не считать такие вещи для match_parent
                        text = text,
                        maxWidth = limitedTextWidth,
                        byLayout = byLayout
                    )
                    lineLastSymbolIndex = lastIndex
                    maxOf(width, minTextWidth)
                }
                else -> {
                    val width = params.paint.getTextWidth(text = text, byLayout = byLayout)
                    maxOf(minOf(width, limitedTextWidth), minTextWidth)
                }
            }

            return PrecomputedLayoutData(
                availableWidth = availableWidth,
                precomputedTextWidth = precomputedTextWidth,
                lineLastSymbolIndex = lineLastSymbolIndex,
                hasAffectingSymbols = byLayout,
                isBoring = isBoring
            ).also { data ->
                precomputedData = data
                refreshPrecomputedCount += 1
                isPrecomputedActual = true
            }
        }
    }

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
            isStaticTouchRect = rect != null
            touchRect.set(rect ?: this@TextLayout.rect)
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
    internal inner class DrawableStateHelper {

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
         * Установить активное состояние текстовой разметки.
         */
        fun setActivated(activated: Boolean) {
            updateDrawableState(android.R.attr.state_activated, activated)
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
            textBackgroundPath.addRect(if (isSimpleDrawing) layoutRect else clipRect, Path.Direction.CW)
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
     * Подготовленные данные для построения [Layout].
     *
     * @property availableWidth допустимая ширина, под которую производились вычисления.
     * @property precomputedTextWidth посчитанная ширина текста для построения [Layout].
     * @property lineLastSymbolIndex индекс последнего символа в строке.
     * @property hasAffectingSymbols проверка наличия в тексте символов влияющих на ширину строки.
     */
    private data class PrecomputedLayoutData(
        val availableWidth: Int?,
        val precomputedTextWidth: Int,
        val lineLastSymbolIndex: Int? = null,
        val hasAffectingSymbols: Boolean? = null,
        val isBoring: BoringLayout.Metrics? = null
    )

    /**
     * Модель снимка внутреннего состояния [TextLayout].
     * @see TextLayout.params
     * @see TextLayout._layout
     * @see TextLayout.isLayoutChanged
     * @see TextLayout.layoutRect
     * @see TextLayout.refreshPrecomputedCount
     * @see TextLayout.fadingEdgeRule
     * @see TextLayout.isFadeEdgeVisible
     */
    internal class StateSnapshot(
        val params: TextLayoutParams,
        val layout: Layout?,
        val isLayoutChanged: Boolean,
        val textPos: Pair<Float, Float>,
        val refreshPrecomputedCount: Int,
        val fadingEdgeRule: Boolean,
        val isFadeEdgeVisible: Boolean
    )

    /**
     * Модель снимка внутреннего состояния [CachedState].
     */
    internal class CachedStateSnapshot(
        val resetCount: Int,
        val configuredTextCount: Int,
        val horizontalPaddingCount: Int,
        val verticalPaddingCount: Int,
        val textWidthCount: Int,
        val maxTextWidthCount: Int,
        val minTextWidthCount: Int,
        val minHeightByLinesCount: Int,
        val layoutMaxHeightCount: Int,
        val isVisibleCount: Int,
        val widthCount: Int,
        val heightCount: Int
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
private const val DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE = 1
private const val DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE = 300
private const val DEFAULT_AUTO_SIZE_STEP_GRANULARITY = 1
internal const val BORING_LAYOUT_TEXT_LENGTH_LIMIT = 40