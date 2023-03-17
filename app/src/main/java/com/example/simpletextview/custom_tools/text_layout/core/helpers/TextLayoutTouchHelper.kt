package com.example.simpletextview.custom_tools.text_layout.core.helpers

import android.graphics.Rect
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.example.simpletextview.custom_tools.text_layout.TextLayout
import kotlin.math.roundToInt

/**
 * Вспомогательный класс для обработки касаний по [TextLayout].
 */
internal class TextLayoutTouchHelper(
    private val target: TextLayout,
    private val drawableStateHelper: TextLayoutDrawableStateHelper
) {

    private var parentView: View? = null

    private val touchRect: Rect = Rect()
    private var isStaticTouchRect = false

    private var gestureDetector: GestureDetector? = null
        get() {
            if (field == null) {
                field = parentView?.context?.let {
                    object : GestureDetector(it, gestureListener) {
                        override fun onTouchEvent(ev: MotionEvent): Boolean =
                            if (ev.action == MotionEvent.ACTION_MOVE && drawableStateHelper.isPressedState) {
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
                if (!drawableStateHelper.isEnabled || !isConfirmed) return@also

                val context = parentView?.context ?: return@also
                onClickListener?.onClick(context, target)
            }

        override fun onLongPress(event: MotionEvent) {
            if (isInTouchRect(event) && drawableStateHelper.isEnabled) {
                val context = parentView?.context ?: return
                onLongClickListener?.onLongClick(context, target)?.also {
                    parentView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
        }

        fun onMove(event: MotionEvent) = isInTouchRect(event)
    }

    var touchPaddingLeft = 0
        private set
    var touchPaddingTop = 0
        private set
    var touchPaddingRight = 0
        private set
    var touchPaddingBottom = 0
        private set

    var onClickListener: TextLayout.OnClickListener? = null
        private set
    var onLongClickListener: TextLayout.OnLongClickListener? = null
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
     */
    fun setOnClickListener(listener: TextLayout.OnClickListener?) {
        onClickListener = listener
    }

    /**
     * Установить слушателя долгих кликов [listener] по текстовой разметке.
     */
    fun setOnLongClickListener(listener: TextLayout.OnLongClickListener?) {
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
        left: Int = touchPaddingLeft,
        top: Int = touchPaddingTop,
        right: Int = touchPaddingRight,
        bottom: Int = touchPaddingBottom
    ) {
        touchPaddingLeft = left
        touchPaddingTop = top
        touchPaddingRight = right
        touchPaddingBottom = bottom
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
    fun setStaticTouchRect(rect: Rect, isStatic: Boolean) {
        isStaticTouchRect = isStatic
        touchRect.set(rect)
    }

    /**
     * Обновить область касания согласно [TextLayout.rect].
     *
     * Игнорируется, если установлена статичная область касания [setStaticTouchRect].
     */
    fun updateTouchRect(rect: Rect) {
        if (isStaticTouchRect) return
        with(rect) {
            touchRect.set(
                left - touchPaddingLeft,
                top - touchPaddingTop,
                right + touchPaddingRight,
                bottom + touchPaddingBottom
            )
        }
    }

    /**
     * Обработать событие касания [event].
     * @return true, если событие касания было обработано текущей текстовой разметкой.
     */
    fun onTouch(event: MotionEvent): Boolean =
        gestureDetector?.onTouchEvent(event)
            ?.also { isHandled -> drawableStateHelper.updatePressedStateByTouch(event.action, isHandled) }
            ?.takeIf { onClickListener != null || onLongClickListener != null }
            ?: false

    fun onTouchCanceled() {
        drawableStateHelper.updatePressedStateByTouch(MotionEvent.ACTION_CANCEL, true)
    }
}