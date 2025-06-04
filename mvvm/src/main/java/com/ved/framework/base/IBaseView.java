package com.ved.framework.base;

import android.content.Context;
import android.os.Bundle;

import com.trello.rxlifecycle4.LifecycleProvider;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by ved on 2017/6/15.
 */

public interface IBaseView<V extends ViewDataBinding, VM extends BaseViewModel> {
    /**
     * 初始化界面传递参数
     */
    void initParam();
    /**
     * 初始化数据
     */
    void initData();

    /**
     * 初始化界面观察者的监听
     */
    void initViewObservable();

    VM ensureViewModelCreated();

    boolean isSwipeBack();

    boolean isRegisterEventBus();

    void initView();

    void requestCallPhone(boolean denied);

    V getBinding(Bundle savedInstanceState);

    void getWifiRssi(boolean pass,int rssi);

    void dismissCustomDialog();

    boolean mvvmDialog();

    void showCustomDialog();

    boolean customDialog();

    FragmentActivity FragmentActivity();

    Context getViewContext();

    LifecycleOwner getLifecycleOwner();

    Lifecycle getViewLifecycle();

    LifecycleProvider<?> getLifecycleProvider();
}
