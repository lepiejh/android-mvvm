package com.ved.framework.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class TouchableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val gestureDetector: GestureDetector
    private var isLongPressPerformed = false

    private var onLongPress: (() -> Unit)? = null
    private var onReleaseAfterLongPress: (() -> Unit)? = null

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                isLongPressPerformed = true
                onLongPress?.invoke()
            }
        })
    }

    // 统一设置回调的高阶函数
    fun setTouchListeners(
        onLongPress: (() -> Unit)? = null,
        onReleaseAfterLongPress: (() -> Unit)? = null
    ) {
        this.onLongPress = onLongPress
        this.onReleaseAfterLongPress = onReleaseAfterLongPress
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_UP -> {
                if (isLongPressPerformed) {
                    onReleaseAfterLongPress?.invoke()
                }
                isLongPressPerformed = false
            }
            MotionEvent.ACTION_CANCEL -> {
                isLongPressPerformed = false
            }
        }

        return true
    }
}