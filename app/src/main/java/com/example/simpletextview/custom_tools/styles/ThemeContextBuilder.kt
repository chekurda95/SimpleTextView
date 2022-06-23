package com.example.simpletextview.custom_tools.styles

import android.R.attr.theme
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat.ID_NULL
import androidx.core.content.res.use
import androidx.fragment.app.Fragment

/**
 * Ключ, по которому укладывается тема в аргументы фрагмента
 *
 * @see themeRes
 */
const val THEME_CONTEXT_BUILDER_FRAGMENT_THEME = "THEME_CONTEXT_BUILDER_FRAGMENT_THEME"

/**
 * Позволяет решить типовую задачу для наших реализаций view: обеспечить наличие атрибутов темизации в контексте с
 * учётом приоритетов.
 *
 * Атрибуты полученной темы разворачиваются _плоским списком_ для view как, если бы они были применены через атрибут
 * [theme]. Это позволяет получить доступ к атрибутам в xml без необходимости применять тему на корневой элемент
 * (невозможно в случай использования корневого тега `<merge>`)
 *
 * @param primaryThemeResolver функция для приоритетного получения стиля
 *
 * @author sv.bubenschikov
 */
class ThemeContextBuilder @JvmOverloads constructor(
    private val context: Context,
    @AttrRes private val defStyleAttr: Int = ID_NULL,
    @StyleRes private val defaultStyle: Int = ID_NULL,
    private var primaryThemeResolver: (() -> Int?)? = null,
    private val fallbackThemeResolver: () -> Int? = {
        context.findInApplicationTheme(defStyleAttr)
    }
) {

    // не заменять на constructor reference, чтобы не ломать отображение в xml
    @VisibleForTesting
    internal var themeContextFactory: (base: Context, styleRes: Int) -> Context = { base, context ->
        ContextThemeWrapper(base, context)
    }

    /**
     * Конструктор для получения контекста с темой для [View]
     *
     * Порядок получения темы:
     * 1. тема из атрибута [theme] в xml
     * 2. тема из атрибута [defStyleAttr] в [context]
     * 3. тема по умолчанию [defaultStyle]
     */
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int = ID_NULL,
        @StyleRes defaultStyle: Int = ID_NULL,
        fallbackThemeResolver: () -> Int? = { context.findInApplicationTheme(defStyleAttr) }
    ) : this(context, defStyleAttr, defaultStyle, primaryThemeResolver = {
        context.getThemeAttributeValue(attrs)
    }, fallbackThemeResolver = fallbackThemeResolver)

    /**
     * Конструктор для получения контекста с темой для [Fragment]
     *
     * Порядок получения темы:
     * 1. тема из аргументов фрагмента [fragment] по ключу [THEME_CONTEXT_BUILDER_FRAGMENT_THEME]
     * 2. тема из атрибута [defStyleAttr] в [context]
     * 3. тема по умолчанию [defaultStyle]
     *
     * @see themeRes
     */
    @JvmOverloads
    constructor(
        context: Context,
        fragment: Fragment,
        @AttrRes defStyleAttr: Int = ID_NULL,
        @StyleRes defaultStyle: Int = ID_NULL
    ) : this(context, fragment.arguments, THEME_CONTEXT_BUILDER_FRAGMENT_THEME, defStyleAttr, defaultStyle)

    /**
     * Конструктор для получения контекста с темой для [Fragment]
     *
     * Порядок получения темы:
     * 1. тема из аргументов [arguments] по ключу [argumentKey]
     * 2. тема из атрибута [defStyleAttr] в [context]
     * 3. тема по умолчанию [defaultStyle]
     *
     * @see themeRes
     */
    @JvmOverloads
    constructor(
        context: Context,
        arguments: Bundle?,
        argumentKey: String,
        @AttrRes defStyleAttr: Int = ID_NULL,
        @StyleRes defaultStyle: Int = ID_NULL
    ) : this(context, defStyleAttr, defaultStyle, primaryThemeResolver = {
        arguments?.getInt(argumentKey, ID_NULL).takeIf { it != ID_NULL }
    })

    /**
     * Возвращает Context с применённой темой.
     */
    @CheckResult
    fun build(): Context =
        themeContextFactory.invoke(context, buildThemeRes())

    /**
     * Возвращает ресурс темы.
     */
    @CheckResult
    fun buildThemeRes(): Int =
        primaryThemeResolver?.invoke()
            ?: context.getDataFromAttrOrNull(defStyleAttr)
            ?: fallbackThemeResolver.invoke()
            ?: defaultStyle
}

/**
 * Расширение для установки аргумента темы во фрагмент
 *
 * @see ThemeContextBuilder
 * @see THEME_CONTEXT_BUILDER_FRAGMENT_THEME
 */
@get:StyleRes
@setparam:StyleRes
var Fragment.themeRes: Int
    get() = arguments?.themeRes ?: ID_NULL
    set(value) {
        requireArguments().themeRes = value
    }

/**
 * @see Fragment.themeRes
 */
@get:StyleRes
@setparam:StyleRes
var Bundle.themeRes: Int
    get() = getInt(THEME_CONTEXT_BUILDER_FRAGMENT_THEME, ID_NULL)
    set(value) {
        putInt(THEME_CONTEXT_BUILDER_FRAGMENT_THEME, value)
    }

@StyleRes
private fun Context.findInApplicationTheme(@AttrRes attr: Int) =
    ContextThemeWrapper(this, applicationInfo.theme).getDataFromAttrOrNull(attr)

@StyleRes
private fun Context.getThemeAttributeValue(attrs: AttributeSet?): Int? = attrs?.let { attrSet ->
    obtainStyledAttributes(attrSet, intArrayOf(android.R.attr.theme)).use { arr ->
        arr.getResourceId(0, ID_NULL)
    }.takeIf { it != ID_NULL }
}