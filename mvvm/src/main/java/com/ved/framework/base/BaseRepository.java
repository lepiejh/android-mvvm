package com.ved.framework.base;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 数据仓库基类：
 * 内置订阅容器，统一管理数据层 RxJava 订阅，避免内存泄漏；
 * 子类只需实现具体的数据获取逻辑（网络/本地）。
 */
public abstract class BaseRepository implements IRepository {
    private CompositeDisposable mCompositeDisposable;

    protected void addSubscribe(Disposable disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(disposable);
    }

    @Override
    public void onCleared() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
        }
    }
}
