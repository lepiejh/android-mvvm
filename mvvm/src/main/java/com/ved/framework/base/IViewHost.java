package com.ved.framework.base;

import android.content.Context;
import android.os.Bundle;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.trello.rxlifecycle4.LifecycleProvider;

/**
 * 视图宿主角色接口（接口隔离原则）：
 * 定义 View 与宿主（Activity / Fragment）环境之间的能力契约，
 * 供 BaseView、生命周期管理、导航等组件按需依赖。
 */
public interface IViewHost<V extends ViewDataBinding, VM extends BaseViewModel> {

    /** 确保 ViewModel 已创建并返回 */
    VM ensureViewModelCreated();

    /** 获取 DataBinding 绑定对象 */
    V getBinding(Bundle savedInstanceState);

    /** 获取宿主 FragmentActivity */
    FragmentActivity FragmentActivity();

    /** 获取视图上下文 */
    Context getViewContext();

    /** 获取生命周期持有者 */
    LifecycleOwner getLifecycleOwner();

    /** 获取视图生命周期 */
    Lifecycle getViewLifecycle();

    /** 获取 RxLifecycle 提供者 */
    LifecycleProvider<?> getLifecycleProvider();

    /** 判断宿主是否为 Fragment */
    default boolean isFragment() {
        return false;
    }

    /** 获取 Fragment 实例（宿主为 Fragment 时） */
    default Fragment getFragment() {
        return null;
    }

    /** 获取当前 Activity 实例 */
    FragmentActivity getCurrentActivity();
}
