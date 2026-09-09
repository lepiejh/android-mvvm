package com.ved.framework.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;

interface Host<V extends ViewDataBinding, VM extends BaseViewModel> extends IBaseView<V, VM> {

    /**
     * 初始化根布局，返回 layoutId。
     * <p>
     * 默认实现见 {@link BaseFragment#initContentView(LayoutInflater, ViewGroup, Bundle)}：
     * 根据 {@code V}（ViewDataBinding 泛型）自动推断布局文件，无需覆写。
     * 仅当布局名与 Binding 类名无法按约定对应时覆写。
     */
    int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    /**
     * 页面数据加载方法（页面第一次加载数据时调用）
     */
    void loadData();
}
