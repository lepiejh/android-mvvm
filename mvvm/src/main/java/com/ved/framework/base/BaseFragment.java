package com.ved.framework.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.trello.rxlifecycle4.LifecycleProvider;
import com.trello.rxlifecycle4.android.FragmentEvent;
import com.trello.rxlifecycle4.components.support.RxFragment;
import com.ved.framework.bus.event.eventbus.MessageEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Fragment 基类（委托模式）：
 * 公共逻辑全部收敛到 {@link FragmentDelegate}，本类只保留 super 生命周期调用与宿主环境方法；
 * showDialog/startActivity 等宿主能力方法由 {@link Host} 的 default 实现统一提供，
 * 与 {@link BaseDialogFragment} 共享同一份契约与委托实现。
 * <p>
 * EventBus 的 {@code @Subscribe} 方法必须保留在本类中：
 * EventBus 反射扫描的是类继承链（getDeclaredMethods），不会扫描接口 default 方法。
 */
public abstract class BaseFragment<V extends ViewDataBinding, VM extends BaseViewModel<? extends BaseModel>>
        extends RxFragment implements Host<V, VM> {

    private final FragmentDelegate<V, VM> delegate = new FragmentDelegate<>(this);

    protected V binding;

    @Override
    public FragmentDelegate<V, VM> getFragmentDelegate() {
        return delegate;
    }

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
    public V getBinding(Bundle savedInstanceState) {
        return binding;
    }

    /**
     * 初始化根布局。
     * <p>
     * 默认实现：根据 {@code V}（ViewDataBinding 泛型）自动推断布局文件，
     * 例如 {@code BaseFragment<HistoricalFragmentBinding, HistoricalViewModel>}
     * 会解析到 {@code R.layout.historical_fragment}，因此无需覆写本方法。
     * 仅当布局名与 Binding 类名无法按约定对应时，覆写本方法返回布局 id。
     */
    @Override
    public abstract int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @Override
    public FragmentActivity FragmentActivity() {
        return BaseFragment.this.getActivity();
    }

    @Override
    public Context getViewContext() {
        return BaseFragment.this.getContext();
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
    public FragmentActivity getCurrentActivity() {
        return super.getActivity();
    }

    @Override
    public LifecycleProvider<FragmentEvent> getLifecycleProvider() {
        return BaseFragment.this;
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
