package com.ved.framework.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;

import com.ved.framework.permission.IPermission;

/**
 * Fragment 宿主契约。
 * <p>
 * {@link BaseFragment} 与 {@link BaseDialogFragment} 的逻辑 100% 相同，唯一差异是父类
 * （{@code RxFragment} / {@code RxDialogFragment}），Java 单继承无法合并两个不同父类。
 * 因此与"Fragment 宿主"语义绑定的能力方法统一上提为本接口的 default 实现，
 * 通过 {@link #getFragmentDelegate()} 转发到共享委托 {@link FragmentDelegate}，
 * 两个基类只需实现宿主环境方法（Activity/Context/Lifecycle）与生命周期桥接。
 * <p>
 * 注意：这些能力方法是对外公开契约，必须声明在 public 接口上——若声明在包私有接口中，
 * 外部包将无法通过 public 子类访问这些方法（JLS 6.6.1）。
 *
 * @param <V>  ViewDataBinding 泛型
 * @param <VM> BaseViewModel 泛型
 */
public interface Host<V extends ViewDataBinding, VM extends BaseViewModel<?>> extends IBaseView<V, VM> {

    /**
     * 获取挂接在宿主上的 Fragment 委托（共享逻辑载体）。
     * <p>
     * 本接口的 default 能力方法均通过它转发，宿主基类只需返回自己持有的委托实例。
     */
    FragmentDelegate<V, VM> getFragmentDelegate();

    /**
     * 初始化根布局，返回 layoutId。
     * <p>
     * 默认实现见 {@link BaseFragment#initContentView(LayoutInflater, ViewGroup, Bundle)}：
     * 根据 {@code V}（ViewDataBinding 泛型）自动推断布局文件，无需覆写。
     * 仅当布局名与 Binding 类名无法按约定对应时覆写。
     */
    int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    /**
     * 页面数据加载方法（页面第一次加载数据时调用），默认空实现，子类按需覆写。
     */
    default void loadData() {
    }

    /** Fragment 宿主恒为 Fragment */
    @Override
    default boolean isFragment() {
        return true;
    }

    /** Fragment 宿主返回自身（本接口的实现方必为 Fragment 子类） */
    @Override
    default Fragment getFragment() {
        return (Fragment) this;
    }

    @Override
    default VM ensureViewModelCreated() {
        return getFragmentDelegate().ensureViewModelCreated();
    }

    @Override
    default boolean needReload() {
        return getFragmentDelegate().needReload();
    }

    @Override
    default void initView() {
        getFragmentDelegate().initView();
    }

    @Override
    default void refreshView() {
        getFragmentDelegate().refreshView();
    }

    /** 显示加载对话框 */
    default void showDialog() {
        getFragmentDelegate().showDialog();
    }

    /** 显示带标题的加载对话框 */
    default void showDialog(String title) {
        getFragmentDelegate().showDialog(title);
    }

    /** 关闭加载对话框 */
    default void dismissDialog() {
        getFragmentDelegate().dismissDialog();
    }

    /** 申请运行时权限 */
    default void requestPermission(IPermission iPermission, String... permissions) {
        getFragmentDelegate().requestPermission(iPermission, permissions);
    }

    /** 跳转页面 */
    default void startActivity(Class<?> clz) {
        getFragmentDelegate().startActivity(clz);
    }

    /** 跳转页面并携带参数 */
    default void startActivity(Class<?> clz, Bundle bundle) {
        getFragmentDelegate().startActivity(clz, bundle);
    }

    /** 跳转页面用于回传数据 */
    default void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        getFragmentDelegate().startActivityForResult(clz, requestCode, bundle);
    }

    /** 跳转通用容器 Activity */
    default void startContainerActivity(String canonicalName) {
        getFragmentDelegate().startContainerActivity(canonicalName);
    }

    /** 跳转通用容器 Activity 并携带参数 */
    default void startContainerActivity(String canonicalName, Bundle bundle) {
        getFragmentDelegate().startContainerActivity(canonicalName, bundle);
    }

    /** 返回键处理（默认不拦截） */
    default boolean isBackPressed() {
        return getFragmentDelegate().isBackPressed();
    }
}
