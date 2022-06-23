package com.example.simpletextview.custom_tools.styles

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.text.Layout
import android.text.TextUtils.TruncateAt
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.ID_NULL
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.RecyclerView
import com.example.simpletextview.R
import com.example.simpletextview.custom_tools.TextLayout
import com.example.simpletextview.custom_tools.styles.CanvasStylesProvider.Companion.paddingAttrs
import com.example.simpletextview.custom_tools.styles.CanvasStylesProvider.Companion.textAttrs
import com.example.simpletextview.custom_tools.styles.StyleParams.PaddingStyle
import com.example.simpletextview.custom_tools.styles.StyleParams.StyleKey
import com.example.simpletextview.custom_tools.styles.StyleParams.TextStyle
import com.example.simpletextview.custom_tools.utils.doOnDetachedFromWindow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Поставщик моделей значений базовых атрибутов стилей текста и отступов.
 *
 * Класс содержит механику obtain`а ресурсов стилей с текстовыми атрибутами [textAttrs] и паддингами [paddingAttrs].
 * При активации работы кэша [isResourceCacheEnabled], кэширует ранее полученные атрибуты стилей для переиспользования.
 * Данная механика позволяет ускорить получение атрибутов xml стилей для ячеек списка ~ в 10 раз,
 * что позволяет создавать разметки текстов [TextLayout] за 50 микросекунд против 300-1000
 * (значения примера для мощного девайса, индивидуальны для разных мощностей девайсов).
 *
 * @see textStyleProvider
 * @see paddingStyleProvider
 *
 * @author vv.chekurda
 */
abstract class CanvasStylesProvider {

    companion object {

        /**
         * Поддерживаемые атрибуты текста.
         * Расширение списка увеличит время [obtainStyle] для получения модели стиля.
         */
        private val textAttrs = intArrayOf(
            android.R.attr.text,
            android.R.attr.textColor,
            android.R.attr.textSize,
            android.R.attr.layout_width,
            android.R.attr.gravity,
            android.R.attr.ellipsize,
            android.R.attr.includeFontPadding,
            android.R.attr.maxLines,
            android.R.attr.visibility,
            android.R.attr.paddingStart,
            android.R.attr.paddingTop,
            android.R.attr.paddingEnd,
            android.R.attr.paddingBottom,
            android.R.attr.fontFamily
        )

        /**
         * Поддерживаемые padding атрибуты.
         * Расширение списка увеличит время [obtainStyle] для получения модели стиля.
         */
        private val paddingAttrs = intArrayOf(
            android.R.attr.paddingStart,
            android.R.attr.paddingTop,
            android.R.attr.paddingEnd,
            android.R.attr.paddingBottom,
            android.R.attr.fontFamily
        )

        /**
         * Получить модель стиля текста [TextStyle] по ресурсу стиля [styleKey].
         * @see StyleKey
         */
        fun obtainTextStyle(
            context: Context,
            styleKey: StyleKey
        ): TextStyle {
            val styleRes = obtainStyleResByKey(context, styleKey)

            var text: String? = null
            var textSize: Int? = null
            var color: Int? = null
            var layoutWidth: Int? = null
            var gravity: Int? = null
            var ellipsize: Int? = null
            var includeFontPadding: Boolean? = null
            var maxLines: Int? = null
            var isVisible: Boolean? = null
            var paddingStyle: PaddingStyle? = null
            var typeface: Typeface? = null

            ContextThemeWrapper(context, styleRes).withStyledAttributes(
                attrs = textAttrs,
                resourceId = styleRes
            ) {
                text = getString(textAttrs.indexOf(android.R.attr.text)) ?: text
                textSize = getDimensionPixelSize(textAttrs.indexOf(android.R.attr.textSize), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                color = getColor(textAttrs.indexOf(android.R.attr.textColor), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                    ?: getResourceId(textAttrs.indexOf(android.R.attr.textColor), R.color.palette_color_black1)
                        .let { ContextCompat.getColor(context, it) }
                layoutWidth = getDimensionPixelSize(textAttrs.indexOf(android.R.attr.layout_width), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                gravity = getInt(textAttrs.indexOf(android.R.attr.gravity), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                ellipsize = getInt(textAttrs.indexOf(android.R.attr.ellipsize), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                maxLines = getInt(textAttrs.indexOf(android.R.attr.maxLines), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                typeface = getResourceId(textAttrs.indexOf(android.R.attr.fontFamily), NO_RESOURCE)
                    .takeIf { it != NO_RESOURCE }
                    ?. let { ResourcesCompat.getFont(context, it) }

                val includeFontPadIndex = textAttrs.indexOf(android.R.attr.includeFontPadding)
                includeFontPadding = if (hasValue(includeFontPadIndex)) {
                    getBoolean(includeFontPadIndex, true)
                } else null

                val visibilityIndex = textAttrs.indexOf(android.R.attr.visibility)
                isVisible = if (hasValue(visibilityIndex)) {
                    getBoolean(visibilityIndex, true)
                } else null

                paddingStyle = createPaddingStyle(this, textAttrs, styleKey)
            }
            val alignment = gravity?.let { mapGravityToAlignment(it) }
            val truncateAt = ellipsize?.let { mapEllipsizeToTruncate(it) }

            return TextStyle(
                styleKey,
                text = text,
                textSize = textSize?.toFloat(),
                textColor = color,
                typeface = typeface,
                layoutWidth = layoutWidth,
                alignment = alignment,
                ellipsize = truncateAt,
                includeFontPad = includeFontPadding,
                maxLines = maxLines,
                isVisible = isVisible,
                paddingStyle = paddingStyle
            )
        }

        /**
         * Получить модель стиля отступов [PaddingStyle] по ключу стиля [styleKey].
         * @see StyleKey
         */
        fun obtainPaddingStyle(
            context: Context,
            styleKey: StyleKey
        ): PaddingStyle {
            val styleRes = obtainStyleResByKey(context, styleKey)
            var style: PaddingStyle? = null

            ContextThemeWrapper(context, styleRes).withStyledAttributes(
                attrs = paddingAttrs,
                resourceId = styleRes
            ) {
                style = createPaddingStyle(this, paddingAttrs, styleKey)
            }

            return style ?: PaddingStyle()
        }

        /**
         * Получить ресурс стиля с помощью [StyleKey].
         */
        private fun obtainStyleResByKey(context: Context, styleKey: StyleKey) =
            if (styleKey.styleAttr != ID_NULL) {
                ThemeContextBuilder(
                    context = context,
                    defStyleAttr = styleKey.styleAttr,
                    defaultStyle = styleKey.styleRes
                ).buildThemeRes()
            } else {
                styleKey.styleRes
            }

        private fun createPaddingStyle(
            typedArray: TypedArray,
            attrs: IntArray,
            styleKey: StyleKey
        ): PaddingStyle = with(typedArray) {
            PaddingStyle(
                styleKey = styleKey,
                paddingStart = getDimensionPixelSize(attrs.indexOf(android.R.attr.paddingStart), 0),
                paddingTop = getDimensionPixelSize(attrs.indexOf(android.R.attr.paddingTop), 0),
                paddingEnd = getDimensionPixelSize(attrs.indexOf(android.R.attr.paddingEnd), 0),
                paddingBottom = getDimensionPixelSize(attrs.indexOf(android.R.attr.paddingBottom), 0)
            )
        }
    }

    /**
     * Кэш стилей с текстовыми атрибутами [textAttrs].
     */
    private val textStyleCollection = ConcurrentHashMap<StyleKey, TextStyle>()

    /**
     * Кэш стилей с padding атрибутами [paddingAttrs].
     */
    private val paddingStyleCollection = ConcurrentHashMap<StyleKey, PaddingStyle>()

    /**
     * Признак наличия подписки на lifecycle [RecyclerView],
     * чтобы избегать многократных установок [View.OnAttachStateChangeListener].
     */
    private var isAttachedToRecycler = false

    /**
     * Включить/выключить кэширование полученных значений атрибутов стилей.
     */
    var isResourceCacheEnabled: Boolean = true

    /**
     * Поставщик моделей стиля текста [TextStyle].
     */
    val textStyleProvider = object : StyleParamsProvider<TextStyle> {
        override fun getStyleParams(context: Context, styleKey: StyleKey): TextStyle =
            obtainStyle(styleKey, textStyleCollection, isResourceCacheEnabled) {
                obtainTextStyle(context, styleKey)
            }.also {
                if (isResourceCacheEnabled && it.paddingStyle != null) {
                    paddingStyleCollection[it.styleKey] = it.paddingStyle
                }
            }
    }

    /**
     * Поставщик моделей стиля отступов [PaddingStyle].
     */
    val paddingStyleProvider = object : StyleParamsProvider<PaddingStyle> {
        override fun getStyleParams(context: Context, styleKey: StyleKey): PaddingStyle =
            obtainStyle(styleKey, paddingStyleCollection, isResourceCacheEnabled) {
                obtainPaddingStyle(context, styleKey)
            }
    }

    /**
     * Очистить закэшированные ссылки.
     */
    fun clearReferences() {
        textStyleCollection.clear()
        paddingStyleCollection.clear()
    }

    /**
     * Активировать кэширование ресурсов стилей при отображении ячеек в [RecyclerView].
     *
     * Иногда obtain атрибутов стилей для всех текстовых разметок view
     * занимает ощутимую долю времени создания всей ячейки,
     * поэтому для списков уместно кэширование.
     */
    fun activateResourceCacheForRecycler(itemRootView: View) {
        if (isAttachedToRecycler) return
        (itemRootView.parent as? RecyclerView)?.let {
            isResourceCacheEnabled = true

            isAttachedToRecycler = true
            it.doOnDetachedFromWindow {
                isAttachedToRecycler = false
                clearReferences()
            }
        } ?: run { isResourceCacheEnabled = false }
    }

    /**
     * Получить модель стиля [STYLE_PARAMS] для стиля текста [styleRes].
     * Если включен кэш [isCacheEnabled], то вернет закэшированный результат.
     * В случае отсутствия результата в кэше [cachedCollection] -
     * стиль будет получен с помощью метода [getStyle] и закэширует результат в [cachedCollection].
     */
    private inline fun <reified STYLE_PARAMS : StyleParams> obtainStyle(
        styleKey: StyleKey,
        cachedCollection: ConcurrentMap<StyleKey, STYLE_PARAMS>,
        isCacheEnabled: Boolean,
        getStyle: () -> STYLE_PARAMS
    ): STYLE_PARAMS = cachedCollection.takeIf { isCacheEnabled }
        ?.get(styleKey)
        ?: getStyle().also {
            if (isCacheEnabled) cachedCollection[styleKey] = it
        }
}

/**
 * Поставщик параметров стиля [STYLE_PARAMS].
 */
interface StyleParamsProvider<STYLE_PARAMS : StyleParams> {

    /**
     * Получить параметры [STYLE_PARAMS] по ресурсу стиля [styleRes].
     */
    fun getStyleParams(context: Context, @StyleRes styleRes: Int): STYLE_PARAMS =
        getStyleParams(context, StyleKey(styleRes))

    /**
     * Получить параметры [STYLE_PARAMS] по ключу стиля [styleKey].
     */
    fun getStyleParams(context: Context, styleKey: StyleKey): STYLE_PARAMS
}

private fun mapGravityToAlignment(gravity: Int): Layout.Alignment =
    when (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
        Gravity.START -> Layout.Alignment.ALIGN_NORMAL
        Gravity.CENTER_HORIZONTAL, Gravity.CENTER -> Layout.Alignment.ALIGN_CENTER
        Gravity.END -> Layout.Alignment.ALIGN_OPPOSITE
        else -> Layout.Alignment.ALIGN_NORMAL
    }

private fun mapEllipsizeToTruncate(ellipsize: Int): TruncateAt =
    when (ellipsize) {
        ELLIPSIZE_START -> TruncateAt.START
        ELLIPSIZE_MIDDLE -> TruncateAt.MIDDLE
        else -> TruncateAt.END
    }

private const val ELLIPSIZE_START = 1
private const val ELLIPSIZE_MIDDLE = 2
private const val NO_RESOURCE = -1