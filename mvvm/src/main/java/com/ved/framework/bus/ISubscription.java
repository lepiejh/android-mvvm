package com.ved.framework.bus;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 订阅容器统一抽象：
 * 屏蔽 RxJava Disposable 的注册、移除与清理细节，
 * 使全局容器（{@link RxSubscriptions}）与 ViewModel 级容器面向同一接口编程。
 */
public interface ISubscription {

    void add(Disposable s);

    void remove(Disposable s);

    boolean isDisposed();

    void clear();

    void dispose();
}
