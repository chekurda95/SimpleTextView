package com.example.simpletextview.custom_tools.utils

import android.view.View

/**
 * Выполняет [action] при отсоединении [View] от окна
 */
@Suppress("unused")
inline fun View.doOnDetachedFromWindow(crossinline action: (view: View) -> Unit) {
    addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                removeOnAttachStateChangeListener(this)
                action(this@doOnDetachedFromWindow)
            }
        }
    )
}