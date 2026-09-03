package com.ved.framework.base;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Model 层基类：
 * 内置订阅容器，统一管理数据层 RxJava 订阅，避免内存泄漏。
 */
public class BaseModel implements IModel {
    private CompositeDisposable mCompositeDisposable;

    public BaseModel() {
    }

    /**
     * 清除所有订阅
     */
    protected void clearSubscriptions() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
        }
    }

    /**
     * 获取 CompositeDisposable（供 ViewModel 使用）
     */
    public CompositeDisposable getCompositeDisposable() {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        return mCompositeDisposable;
    }

    @Override
    public void onCleared() {
        clearSubscriptions();
    }
}