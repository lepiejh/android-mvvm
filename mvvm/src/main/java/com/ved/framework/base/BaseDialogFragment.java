package com.ved.framework.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.trello.rxlifecycle4.LifecycleProvider;
import com.trello.rxlifecycle4.android.FragmentEvent;
import com.trello.rxlifecycle4.components.support.RxDialogFragment;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * DialogFragment 基类（委托模式）：
 * 公共逻辑全部收敛到 {@link FragmentDelegate}，本类只保留 super 生命周期调用与宿主能力方法，
 * 与 {@link BaseFragment} 共享同一份委托实现，唯一差异是父类 {@link RxDialogFragment}。
 */
public abstract class BaseDialogFragment<V extends ViewDataBinding, VM extends BaseViewModel>
        extends RxDialogFragment implements FragmentDelegate.Host<V, VM> {

    private final FragmentDelegate<V, VM> delegate = new FragmentDelegate<>(this);

    protected V binding;

    protected VM getViewModel() {
        return delegate.getViewModel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        delegate.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        delegate.setMenuVisibility(menuVisible);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = delegate.createBinding(inflater, container, savedInstanceState);
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        delegate.onDestroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        delegate.onViewCreated(view, savedInstanceState);
    }

    @Override
    public VM ensureViewModelCreated() {
        return delegate.ensureViewModelCreated();
    }

    @Override
    public boolean needReload() {
        return delegate.needReload();
    }

    @Override
    public void initView() {
        delegate.initView();
    }

    @Override
    public void refreshView() {
        delegate.refreshView();
    }

    @Override
    public V getBinding(Bundle savedInstanceState) {
        return binding;
    }

    @Override
    public FragmentActivity FragmentActivity() {
        return BaseDialogFragment.this.getActivity();
    }

    @Override
    public Context getViewContext() {
        return BaseDialogFragment.this.getContext();
    }

    @Override
    public LifecycleOwner getLifecycleOwner() {
        return getViewLifecycleOwner();
    }

    @Override
    public Lifecycle getViewLifecycle() {
        return getLifecycle();
    }

    @Override
    public boolean isFragment() {
        return true;
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public FragmentActivity getCurrentActivity() {
        return super.getActivity();
    }

    @Override
    public LifecycleProvider<FragmentEvent> getLifecycleProvider() {
        return BaseDialogFragment.this;
    }

    public void loadData() {
    }

    public void showDialog() {
        delegate.showDialog();
    }

    public void showDialog(String title) {
        delegate.showDialog(title);
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        delegate.requestPermission(iPermission, permissions);
    }

    /**
     * 跳转页面
     */
    public void startActivity(Class<?> clz) {
        delegate.startActivity(clz);
    }

    public void startActivity(Class<?> clz, Bundle bundle) {
        delegate.startActivity(clz, bundle);
    }

    public void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        delegate.startActivityForResult(clz, requestCode, bundle);
    }

    public void startContainerActivity(String canonicalName) {
        delegate.startContainerActivity(canonicalName);
    }

    public void startContainerActivity(String canonicalName, Bundle bundle) {
        delegate.startContainerActivity(canonicalName, bundle);
    }

    /**
     * 初始化根布局
     * <p>
     * 默认实现：根据 {@code V}（ViewDataBinding 泛型）自动推断布局文件，
     * 若泛型无法推断（如直接声明 {@code ViewDataBinding}），请覆写本方法返回布局 id。
     */
    @Override
    public int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutId = BindingLayoutResolver.resolveLayoutId(requireContext(), getClass());
        if (layoutId != 0) {
            return layoutId;
        }
        throw new IllegalStateException("无法从 ViewDataBinding 泛型推断布局文件，"
                + "请确认泛型声明为具体的 Binding 类（如 XxxDialogBinding），或覆写 initContentView() 返回布局 id。"
                + "class=" + getClass().getName());
    }

    public void dismissDialog() {
        delegate.dismissDialog();
    }

    public boolean isBackPressed() {
        return delegate.isBackPressed();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBusCome(MessageEvent<?> event) {
        delegate.onEventBusCome(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onStickyEventBusCome(MessageEvent<?> event) {
        delegate.onStickyEventBusCome(event);
    }
}
