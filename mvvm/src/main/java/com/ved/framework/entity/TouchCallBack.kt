package com.ved.framework.entity

import android.view.MotionEvent
import android.view.View

data class TouchCallBack(
    var event: MotionEvent,
    var view: View
)
