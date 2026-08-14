package com.ved.framework.utils;


import android.os.Handler;
import android.os.Message;

import com.ved.framework.base.BaseActivity;

import java.lang.ref.WeakReference;

public abstract class MemoryLeakHandler<T extends BaseActivity> extends Handler {
    private final WeakReference<T> mActivity;

    public MemoryLeakHandler(T activity) {
        mActivity = new WeakReference<T>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        T activity = mActivity.get();
        // 弱引用被回收(Activity 已销毁)时不再回调, 避免子类对 null 的 activity 操作导致 NPE
        if (activity != null) {
            handleMessage(msg, activity);
        }
    }

    protected abstract void handleMessage(Message msg, T activity);
}
