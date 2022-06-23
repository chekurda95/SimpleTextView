package com.example.simpletextview.custom_tools.utils

import android.view.View.MeasureSpec
import androidx.annotation.Px

/**
 * Утилиты для создания [MeasureSpec].
 *
 * @author vv.chekurda
 */
object MeasureSpecUtils {

    /**
     * Создать [MeasureSpec] с модом [MeasureSpec.AT_MOST] для измерения view c ограничением в [size].
     */
    fun makeAtMostSpec(@Px size: Int): Int =
        MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST)

    /**
     * Создать [MeasureSpec] с модом [MeasureSpec.UNSPECIFIED] для измерения view в размерах, которые ей необходимы.
     */
    fun makeUnspecifiedSpec(): Int =
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

    /**
     * Создать [MeasureSpec] с модом [MeasureSpec.EXACTLY] для измерения view в размере [size].
     */
    fun makeExactlySpec(@Px size: Int): Int =
        MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
}