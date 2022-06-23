package com.example.simpletextview.custom_tools.styles

import android.graphics.Typeface
import android.text.Layout
import android.text.TextUtils.TruncateAt
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat.ID_NULL
import org.apache.commons.lang3.StringUtils.EMPTY

/**
 * Параметры стиля.
 *
 * @property styleKey ключ стиля.
 *
 * @author vv.chekurda
 */
sealed class StyleParams(val styleKey: StyleKey) {

    /**
     * Ключ стиля.
     *
     * @property styleRes ресурс стиля.
     * @property styleAttr атрибут для получения стиля из темы. В качестве дефолта будет использован [styleRes].
     * @property tag тег стиля. Атрибуты одного стиля [styleRes] могут зависеть от разных тем,
     * поэтому для кеширования в таких сценариях необходим дополнительный [tag].
     */
    data class StyleKey(
        @StyleRes val styleRes: Int,
        @AttrRes val styleAttr: Int = ID_NULL,
        val tag: String = EMPTY
    )

    /**
     * Параметры стиля текста.
     *
     * @property text текст.
     * @property textSize размер текста.
     * @property textColor цвет текста.
     * @property layoutWidth ширина разметки.
     * @property alignment мод выравнивания текста.
     * @property ellipsize мод сокращения текста.
     * @property includeFontPad включить стандартные отступы шрифта.
     * @property maxLines максимальное количество строк.
     * @property paddingStyle модель стиля отступов.
     * @property isVisible состояние видимости.
     * @property typeface используемый шрифт.
     */
    class TextStyle(
        styleKey: StyleKey,
        val text: String? = null,
        @Px val textSize: Float? = null,
        @ColorInt val textColor: Int? = null,
        val typeface: Typeface? = null,
        @Px val layoutWidth: Int? = null,
        val alignment: Layout.Alignment? = null,
        val ellipsize: TruncateAt? = null,
        val includeFontPad: Boolean? = null,
        val maxLines: Int? = null,
        val paddingStyle: PaddingStyle? = null,
        val isVisible: Boolean? = null
    ) : StyleParams(styleKey)

    /**
     * Параметры стиля отступов.
     *
     * @property paddingStart левый отступ.
     * @property paddingTop верхний отступ.
     * @property paddingEnd правый отступ.
     * @property paddingBottom нижний отступ.
     */
    class PaddingStyle(
        styleKey: StyleKey = StyleKey(ID_NULL),
        @Px val paddingStart: Int = 0,
        @Px val paddingTop: Int = 0,
        @Px val paddingEnd: Int = 0,
        @Px val paddingBottom: Int = 0
    ) : StyleParams(styleKey)
}
