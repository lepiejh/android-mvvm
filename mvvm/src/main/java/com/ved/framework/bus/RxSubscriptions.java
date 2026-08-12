package com.ved.framework.bus;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 全局订阅容器：
 * 同时暴露静态方法（兼容旧调用）与实例方法（面向 {@link ISubscription} 接口编程）。
 */
public class RxSubscriptions implements ISubscription {
    private static final RxSubscriptions INSTANCE = new RxSubscriptions();
    private final CompositeDisposable mSubscriptions = new CompositeDisposable();

    private RxSubscriptions() {
    }

    public static RxSubscriptions getInstance() {
        return INSTANCE;
    }

    public static boolean isDisposed() {
        return INSTANCE.isDisposed();
    }

    public static void add(Disposable s) {
        INSTANCE.add(s);
    }

    public static void remove(Disposable s) {
        INSTANCE.remove(s);
    }

    public static void clear() {
        INSTANCE.clear();
    }

    public static void dispose() {
        INSTANCE.dispose();
    }

    @Override
    public boolean isDisposed() {
        return mSubscriptions.isDisposed();
    }

    @Override
    public void add(Disposable s) {
        if (s != null) {
            mSubscriptions.add(s);
        }
    }

    @Override
    public void remove(Disposable s) {
        if (s != null) {
            mSubscriptions.remove(s);
        }
    }

    @Override
    public void clear() {
        mSubscriptions.clear();
    }

    @Override
    public void dispose() {
        mSubscriptions.dispose();
    }
}
