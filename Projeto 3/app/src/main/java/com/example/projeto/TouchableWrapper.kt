package com.example.projeto

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class TouchableWrapper @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Desativa a interceptação de eventos de toque
        parent?.requestDisallowInterceptTouchEvent(true)
        return false
    }
}