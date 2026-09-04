package com.ved.framework.bus;


import com.ved.framework.utils.KLog;

import io.reactivex.rxjava3.observers.DisposableObserver;

/**
 * 为RxBus使用的Subscriber, 主要提供next事件的try,catch
 */
public abstract class RxBusSubscriber<T> extends DisposableObserver<T> {

    @Override
    public void onNext(T t) {
        try {
            onEvent(t);
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
    }

    @Override
    public void onComplete() {
    }

    @Override
    public void onError(Throwable e) {
        KLog.e(e.getMessage());
    }

    protected abstract void onEvent(T t);
}
