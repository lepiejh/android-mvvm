package com.ved.framework.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.trello.rxlifecycle4.LifecycleProvider;
import com.trello.rxlifecycle4.android.ActivityEvent;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseActivity<V extends ViewDataBinding, VM extends BaseViewModel> extends ImmersionBarBaseActivity implements IBaseView<V, VM> {
    private final BaseView<V, VM> baseView = new BaseView<>(this);

    protected V binding;
    private final ViewModelDelegate<VM> viewModelDelegate = new ViewModelDelegate<>(this);

    protected VM getViewModel() {
        return viewModelDelegate.getViewModel();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BaseActivity.this.initParam();
        super.onCreate(savedInstanceState);
        baseView.initialize(savedInstanceState);
    }

    @Override
    public VM ensureViewModelCreated() {
        return viewModelDelegate.ensureViewModelCreated();
    }

    @Override
    public V getBinding(Bundle savedInstanceState) {
        BaseActivity.this.binding = DataBindingUtil.setContentView(this, initContentView(savedInstanceState));
        return BaseActivity.this.binding;
    }

    /**
     * 初始化根布局。
     * <p>
     * 默认实现：根据 {@code V}（ViewDataBinding 泛型）自动推断布局文件，
     * 会解析到 {@code R.layout.historical_details_activity}，因此无需覆写本方法。
     * 仅当布局名与 Binding 类名无法按约定对应（如 {@link ContainerActivity}）时，覆写本方法返回布局 id。
     *
     * @return 布局layout的id
     */
    public int initContentView(Bundle savedInstanceState) {
        return BindingLayoutResolver.resolveLayoutIdOrThrow(this, getClass());
    }

    @Override
    public FragmentActivity FragmentActivity() {
        return BaseActivity.this;
    }

    @Override
    public Context getViewContext() {
        return BaseActivity.this;
    }

    @Override
    public LifecycleOwner getLifecycleOwner() {
        return BaseActivity.this;
    }

    @Override
    public Lifecycle getViewLifecycle() {
        return BaseActivity.this.getLifecycle();
    }

    @Override
    public FragmentActivity getCurrentActivity() {
        return this;
    }

    @Override
    public LifecycleProvider<ActivityEvent> getLifecycleProvider() {
        return BaseActivity.this;
    }

    /**
     * 是否注册广播
     */
    protected boolean isReceiver() {
        return false;
    }

    /**
     * 接收广播
     */
    public void onReceive(Intent intent) {
    }

    @Override
    protected void onDestroy() {
        KLog.i(this.getLocalClassName() + " : onDestroy()");
        super.onDestroy();
        baseView.onDestroy();
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

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    public void startActivity(Class<?> clz) {
        baseView.startActivity(clz);
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        baseView.startActivity(clz, bundle);
    }

    public void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        baseView.startActivityForResult(clz, requestCode, bundle);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     */
    public void startContainerActivity(String canonicalName) {
        baseView.startContainerActivity(canonicalName);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     * @param bundle        跳转所携带的信息
     */
    public void startContainerActivity(String canonicalName, Bundle bundle) {
        baseView.startContainerActivity(canonicalName, bundle);
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        baseView.requestPermission(iPermission, permissions);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBusCome(MessageEvent<?> event) {
        if (event != null && viewModelDelegate.hasViewModel()) {
            viewModelDelegate.getCreatedViewModel().receiveEvent(event);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onStickyEventBusCome(MessageEvent<?> event) {
        if (event != null && viewModelDelegate.hasViewModel()) {
            viewModelDelegate.getCreatedViewModel().receiveStickyEvent(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        KLog.i(this.getLocalClassName() + " : onResume()");
    }

    @Override
    protected void onPause() {
        KLog.i(this.getLocalClassName() + " : onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        KLog.i(this.getLocalClassName() + " : onStop()");
        super.onStop();
    }
}
