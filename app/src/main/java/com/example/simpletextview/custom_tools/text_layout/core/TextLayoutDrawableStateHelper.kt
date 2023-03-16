package com.example.simpletextview.custom_tools.text_layout.core

import android.content.res.ColorStateList
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.AttrRes

/**
 * Вспомогательный класс для управления рисуемыми состояниями текстовой разметки.
 * @see colorStateList
 */
internal class TextLayoutDrawableStateHelper(var textPaint: TextPaint) {

    /**
     * Список текущих рисуемых состояний текстовой разметки.
     */
    private val drawableState = mutableSetOf(android.R.attr.state_enabled)
    private var parentView: View? = null

    /**
     * Действие отмены нажатого рисуемого состояния.
     */
    private val cancelPressedCallback = Runnable { isPressed = false }

    /**
     * Получить состояние нажатости.
     */
    val isPressedState: Boolean
        get() = drawableState.contains(android.R.attr.state_pressed)

    /**
     * Установить/получить список цветов текста для состояний.
     * @see isEnabled
     * @see isPressed
     * @see isSelected
     */
    var colorStateList: ColorStateList? = null
        set(value) {
            val isChanged = value != field
            field = value
            if (isChanged) updateTextColorByState()
        }

    /**
     * Установить/получить состояние доступности тестовой разметки.
     *
     * @see colorStateList
     */
    var isEnabled: Boolean = true
        set(value) {
            val isChanged = field != value
            field = value
            if (isChanged) updateEnabled(value)
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
            if (isChanged) updatePressed(value)
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
            if (isChanged) updateSelected(value)
        }

    /**
     * Проинициализировать помощника.
     *
     * @param parentView view, в которой находится текстовая разметка.
     */
    fun init(parentView: View) {
        this.parentView = parentView
    }

    /**
     * Обновить состояние нажатости по действию события касания [motionAction]
     * и признаку обработки этого события [isHandled].
     */
    fun updatePressedStateByTouch(motionAction: Int, isHandled: Boolean) {
        when (motionAction and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN
            -> {
                if (isHandled) {
                    removeCancelPressedCallback()
                    if (isEnabled) isPressed = true
                }
            }
            MotionEvent.ACTION_UP -> dispatchCancelPressedCallback()
            MotionEvent.ACTION_CANCEL -> isPressed = false
        }
    }

    /**
     * Установить доступное состояние тестовой разметки.
     */
    private fun updateEnabled(enabled: Boolean) {
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
    private fun updatePressed(pressed: Boolean) {
        updateDrawableState(android.R.attr.state_pressed, pressed)
    }

    /**
     * Установить выбранное состояние текстовой разметки.
     */
    private fun updateSelected(selected: Boolean) {
        updateDrawableState(android.R.attr.state_selected, selected)
    }

    /**
     * Обновить рисуемое состояние текстовой разметки.
     *
     * @param stateAttr атрибут нового состояния
     * @param isActive true, если состояние активно
     */
    private fun updateDrawableState(@AttrRes stateAttr: Int, isActive: Boolean) {
        val isStateChanged = if (isActive) {
            drawableState.add(stateAttr)
        } else {
            drawableState.remove(stateAttr)
        }

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
     * Отправить отложенное действие [cancelPressedCallback] для отмены нажатого рисуемого состояния.
     */
    private fun dispatchCancelPressedCallback() {
        parentView?.handler?.postDelayed(
            cancelPressedCallback,
            ViewConfiguration.getPressedStateDuration().toLong()
        )
    }

    /**
     * Очистить колбэк для отмены нажатого рисуемого состояния [cancelPressedCallback].
     */
    private fun removeCancelPressedCallback() {
        parentView?.handler?.removeCallbacks(cancelPressedCallback)
    }
}