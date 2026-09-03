package com.ved.framework.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.ved.framework.base.BaseActivity;

import java.lang.ref.WeakReference;

/**
 * 防内存泄漏 Handler
 * 使用 WeakReference 持有 Activity，并自动处理 Activity 销毁后的消息清理
 */
public abstract class MemoryLeakHandler<T extends BaseActivity<?,?>> extends Handler {
    private final WeakReference<T> mActivity;
    private boolean mReleased = false;

    public MemoryLeakHandler(T activity) {
        this(activity, Looper.getMainLooper());
    }

    public MemoryLeakHandler(T activity, Looper looper) {
        super(looper);
        mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        if (mReleased) {
            return;
        }

        T activity = mActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            // Activity 已销毁，自动清理
            release();
            return;
        }

        try {
            handleMessage(msg, activity);
        } catch (Exception e) {
            KLog.e("MemoryLeakHandler handleMessage error: " + e.getMessage());
        }
    }

    /**
     * 处理消息（子类实现）
     */
    protected abstract void handleMessage(Message msg, T activity);

    /**
     * 释放 Handler
     */
    public void release() {
        if (!mReleased) {
            mReleased = true;
            removeCallbacksAndMessages(null);
            mActivity.clear();
        }
    }

    /**
     * 是否已释放
     */
    public boolean isReleased() {
        return mReleased;
    }

    // ==================== 安全的发送方法 ====================

    public boolean sendMessageSafe(Message msg) {
        if (mReleased || isActivityAlive()) {
            return false;
        }
        return sendMessage(msg);
    }

    public boolean sendMessageDelayedSafe(Message msg, long delayMillis) {
        if (mReleased || isActivityAlive()) {
            return false;
        }
        return sendMessageDelayed(msg, delayMillis);
    }

    public boolean sendEmptyMessageSafe(int what) {
        if (mReleased || isActivityAlive()) {
            return false;
        }
        return sendEmptyMessage(what);
    }

    public boolean sendEmptyMessageDelayedSafe(int what, long delayMillis) {
        if (mReleased || isActivityAlive()) {
            return false;
        }
        return sendEmptyMessageDelayed(what, delayMillis);
    }

    private boolean isActivityAlive() {
        T activity = mActivity.get();
        return activity == null || activity.isFinishing() || activity.isDestroyed();
    }
}