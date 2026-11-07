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
interface IViewHost<V extends ViewDataBinding, VM extends BaseViewModel<?>> {

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

    /**
     * 宿主双分派（Visitor 思想）：
     * 根据宿主类型把调用分派给 Fragment 或 Activity 侧的回调，
     * 两者都不可用时什么都不做。
     * <p>
     * 框架内多处（NavigatorHelper / PermissionHelper / BaseView 等）都存在
     * {@code isFragment() && getFragment() != null ? Fragment 分支 : Activity 分支}
     * 的同构判断链，新增宿主类型时需要逐处修改；收敛到本方法后，
     * 调用方只需提供两个回调，判断链只有这一份实现。
     *
     * @param fragmentAction 宿主为 Fragment 时执行
     * @param activityAction 宿主为 Activity 时执行
     */
    default void dispatchToHost(HostAction<Fragment> fragmentAction, HostAction<FragmentActivity> activityAction) {
        if (isFragment() && getFragment() != null) {
            fragmentAction.accept(getFragment());
        } else if (FragmentActivity() != null) {
            activityAction.accept(FragmentActivity());
        }
    }

    /**
     * 宿主动作回调（自定义函数式接口）：
     * 不用 {@code java.util.function.Consumer} 是因为它要求 API 24+，
     * 本框架 minSdk 19 且未开启 core library desugaring，低版本设备会 NoClassDefFoundError。
     */
    interface HostAction<T> {
        void accept(T host);
    }
}
