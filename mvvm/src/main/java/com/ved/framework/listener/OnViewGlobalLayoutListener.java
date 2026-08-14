package com.ved.framework.listener;

import android.view.View;
import android.view.ViewTreeObserver;

public class OnViewGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
    private final int maxHeight;
    private final View view;

    public OnViewGlobalLayoutListener(View view,int maxHeight) {
        this.maxHeight = maxHeight;
        this.view = view;
    }

    @Override
    public void onGlobalLayout() {
        int height = view.getHeight();
        if (height > maxHeight) {
            view.getLayoutParams().height = maxHeight;
            view.requestLayout();
        }
        if (height > 0) {
            // 修复：完成首次测量后移除监听，避免布局循环导致无限回调
            view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    }
}
