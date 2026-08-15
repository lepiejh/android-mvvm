package com.ved.framework.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.KLog;

/**
 * Fragment 委托组件（委托模式 + 组合优先于继承）：
 * <p>
 * {@link BaseFragment} 与 {@link BaseDialogFragment} 的逻辑 100% 相同，唯一差异是父类
 * （{@code RxFragment} / {@code RxDialogFragment}），Java 单继承无法合并两个不同父类，
 * 因此将公共逻辑下沉到本委托类，宿主 Fragment 仅保留 super 生命周期调用与宿主能力方法。
 * 这与 {@link ViewModelDelegate} / {@link BaseView} 的委托风格保持一致。
 *
 * @param <V>  ViewDataBinding 泛型
 * @param <VM> BaseViewModel 泛型
 */
public class FragmentDelegate<V extends ViewDataBinding, VM extends BaseViewModel> {

    /**
     * 宿主契约：委托类执行公共流程时，从宿主 Fragment 提取差异点（模板方法）。
     */
    public interface Host<V extends ViewDataBinding, VM extends BaseViewModel> extends IBaseView<V, VM> {

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

    private final Host<V, VM> host;
    private final BaseView<V, VM> baseView;
    private final ViewModelDelegate<VM> viewModelDelegate;

    protected boolean menuVisibleTag = false;
    protected boolean isLoadData = false;

    public FragmentDelegate(Host<V, VM> host) {
        this.host = host;
        this.baseView = new BaseView<>(host);
        this.viewModelDelegate = new ViewModelDelegate<>(host);
    }

    /**
     * 生命周期：onCreate，调用宿主的参数初始化
     */
    public void onCreate(Bundle savedInstanceState) {
        host.initParam();
    }

    /**
     * 生命周期：menu 可见性变化
     */
    public void setMenuVisibility(boolean menuVisible) {
        menuVisibleTag = menuVisible;
    }

    /**
     * 创建 DataBinding 并返回（宿主负责赋值给 {@code binding} 字段并返回根 View）
     */
    public V createBinding(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return DataBindingUtil.inflate(inflater,
                host.initContentView(inflater, container, savedInstanceState), container, false);
    }

    /**
     * 生命周期：onDestroy
     */
    public void onDestroy() {
        baseView.onDestroy();
    }

    /**
     * 生命周期：onViewCreated，初始化 DataBinding / 事件订阅
     */
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        baseView.initialize(savedInstanceState);
    }

    /**
     * 获取 ViewModel
     */
    public VM getViewModel() {
        return viewModelDelegate.getViewModel();
    }

    /**
     * 确保 ViewModel 已创建（Activity/Fragment 首次创建 ViewModel 时调用）
     */
    public VM ensureViewModelCreated() {
        return viewModelDelegate.ensureViewModelCreated();
    }

    /**
     * 是否需要重新加载数据（Fragment 懒加载场景由子类覆写）
     */
    public boolean needReload() {
        return true;
    }

    /**
     * 初始化 View，配合 {@link #needReload()} 实现 Fragment 懒加载
     */
    public void initView() {
        if (needReload()) {
            KLog.i(host.getClass().getSimpleName() + " menuVisibleTag ：" + menuVisibleTag + ", isLoadData : " + isLoadData);
            if (menuVisibleTag && !isLoadData) {
                isLoadData = true;
                refreshView();
            }
        } else {
            refreshView();
        }
    }

    /**
     * 刷新页面数据
     */
    public void refreshView() {
        // 页面数据初始化方法
        host.initData();
        host.loadData();
    }

    /**
     * 返回键处理（默认不拦截）
     */
    public boolean isBackPressed() {
        return false;
    }

    public void showDialog() {
        baseView.showDialog();
    }

    public void showDialog(String title) {
        baseView.showDialog(title);
    }

    public void dismissDialog() {
        baseView.dismissDialog();
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        baseView.requestPermission(iPermission, permissions);
    }

    public void startActivity(Class<?> clz) {
        baseView.startActivity(clz);
    }

    public void startActivity(Class<?> clz, Bundle bundle) {
        baseView.startActivity(clz, bundle);
    }

    public void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        baseView.startActivityForResult(clz, requestCode, bundle);
    }

    public void startContainerActivity(String canonicalName) {
        baseView.startContainerActivity(canonicalName);
    }

    public void startContainerActivity(String canonicalName, Bundle bundle) {
        baseView.startContainerActivity(canonicalName, bundle);
    }

    /**
     * EventBus 普通事件分发到 ViewModel
     */
    public void onEventBusCome(MessageEvent<?> event) {
        if (event != null && viewModelDelegate.hasViewModel()) {
            viewModelDelegate.getCreatedViewModel().receiveEvent(event);
        }
    }

    /**
     * EventBus 粘性事件分发到 ViewModel
     */
    public void onStickyEventBusCome(MessageEvent<?> event) {
        if (event != null && viewModelDelegate.hasViewModel()) {
            viewModelDelegate.getCreatedViewModel().receiveStickyEvent(event);
        }
    }
}
