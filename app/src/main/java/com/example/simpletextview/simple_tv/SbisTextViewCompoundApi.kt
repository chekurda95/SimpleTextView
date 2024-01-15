package com.example.simpletextview.simple_tv

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

/**
 * API компонента [SbisTextView] по присоединным [Drawable] ко всем 4ем сторонам текста.
 *
 * @author vv.chekurda
 */
interface SbisTextViewCompoundApi {

    /**
     * Устанавливает размер отступа между составными элементами drawable и текстом.
     * @see setCompoundDrawables
     */
    var compoundDrawablePadding: Int

    /**
     * Вернуть paddingStart + [compoundDrawablePadding] для левого [Drawable], если таковой имеется.
     * @see setCompoundDrawables
     */
    val compoundPaddingStart: Int

    /**
     * Вернуть paddingTop + [compoundDrawablePadding] для верхнего [Drawable], если таковой имеется.
     * @see setCompoundDrawables
     */
    val compoundPaddingTop: Int

    /**
     * Вернуть paddingEnd + [compoundDrawablePadding] для правого [Drawable], если таковой имеется.
     * @see setCompoundDrawables
     */
    val compoundPaddingEnd: Int

    /**
     * Вернуть paddingBottom + [compoundDrawablePadding] для нижнего [Drawable], если таковой имеется.
     * @see setCompoundDrawables
     */
    val compoundPaddingBottom: Int

    /**
     * Вернуть массив [Drawable], которые присоединены к 4ем сторонам текста соответственно порядку:
     * левая, верхняя, правая, нижняя.
     * @see setCompoundDrawables
     */
    val compoundDrawables: Array<Drawable?>

    /**
     * Устанавливает Drawables так, чтобы они отображались слева, сверху, справа и под текстом.
     * Используйте null если вам не нужен [Drawable].
     * Вызов этого метода перезапишет все Drawables ранее установленные drawables.
     *
     * @param useIntrinsicBounds true, чтобы у всех [Drawable]
     * использовались [Drawable.getIntrinsicWidth] и [Drawable.getIntrinsicHeight] в качестве ширины и высоты.
     * В ином случае (по умолчанию) у [Drawable] уже должен быть вызван [Drawable.setBounds].
     */
    fun setCompoundDrawables(
        start: Drawable? = null,
        top: Drawable? = null,
        end: Drawable? = null,
        bottom: Drawable? = null,
        useIntrinsicBounds: Boolean = false
    )

    /**
     * Устанавливает Drawables по индентификаторам ресурсов аналогично основному методу [setCompoundDrawables].
     */
    fun setCompoundDrawables(
        @DrawableRes start: Int? = null,
        @DrawableRes top: Int? = null,
        @DrawableRes end: Int? = null,
        @DrawableRes bottom: Int? = null,
        useIntrinsicBounds: Boolean = false
    )
}