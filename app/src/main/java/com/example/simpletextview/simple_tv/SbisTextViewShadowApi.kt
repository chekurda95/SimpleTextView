package com.example.simpletextview.simple_tv

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import com.example.simpletextview.R

/**
 * API компонента [SbisTextView] по теням для текста.
 * @see TextView.setShadowLayer
 *
 * @author vv.chekurda
 */
interface SbisTextViewShadowApi {

    /**
     * Установить для текста тень указанного радиуса размытия [radius] и цвета [color],
     * а также смещения по [dx] и [dy] от положения текста.
     *
     * Создаваемая тень текста не взаимодействует со свойствами,
     * которые отвечают за тени в реальном времени [View.getElevation] и [View.getTranslationZ].
     *
     * См. атрибуты:
     * @see R.styleable.SbisTextView_android_shadowColor
     * @see R.styleable.SbisTextView_android_shadowRadius
     * @see R.styleable.SbisTextView_android_shadowDx
     * @see R.styleable.SbisTextView_android_shadowDy
     */
    fun setShadowLayer(radius: Float, dx: Float, dy: Float, @ColorInt color: Int)

    /**
     * Цвет тени текста.
     * @see setShadowLayer
     * @see R.styleable.SbisTextView_android_shadowColor
     */
    @ColorInt
    fun getShadowColor(): Int

    /**
     * Радиус размытия тени текста.
     * @see setShadowLayer
     * @see R.styleable.SbisTextView_android_shadowRadius
     */
    fun getShadowRadius(): Float

    /**
     * Смещение по X тени текста.
     * @see setShadowLayer
     * @see R.styleable.SbisTextView_android_shadowDx
     */
    fun getShadowDx(): Float

    /**
     * Смещение по Y тени текста.
     * @see setShadowLayer
     * @see R.styleable.SbisTextView_android_shadowDy
     */
    fun getShadowDy(): Float
}