package com.ved.framework.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.trello.rxlifecycle4.LifecycleProvider;
import com.trello.rxlifecycle4.components.support.RxFragment;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

public abstract class BaseFragment<V extends ViewDataBinding, VM extends BaseViewModel> extends RxFragment implements IBaseView {

    protected boolean menuVisibleTag =false;
    protected boolean isLoadData = false;

    private final BaseView<V, VM> baseView = new BaseView<V, VM>() {

        @Override
        protected boolean isSwipeBack() {
            return false;
        }

        @Override
        protected void initViewObservable() {
            BaseFragment.this.initViewObservable();
        }

        @Override
        protected boolean isRegisterEventBus() {
            return BaseFragment.this.isRegisterEventBus();
        }

        @Override
        protected void initView() {
            if (menuVisibleTag && !isLoadData) {
                isLoadData = true;
                //页面数据初始化方法
                BaseFragment.this.initData();
                BaseFragment.this.loadData();
            }
        }

        @Override
        protected void requestCallPhone(boolean denied) {
            BaseFragment.this.requestCallPhone(denied);
        }

        @Override
        protected void getBinding(V binding) {

        }

        @Override
        protected void getViewModel(VM viewModel) {
            BaseFragment.this.viewModel = viewModel;
        }

        @Override
        protected void dismissCustomDialog() {
            BaseFragment.this.dismissCustomDialog();
        }

        @Override
        protected boolean mvvmDialog() {
            return BaseFragment.this.mvvmDialog();
        }

        @Override
        protected void showCustomDialog() {
            BaseFragment.this.showCustomDialog();
        }

        @Override
        protected boolean customDialog() {
            return BaseFragment.this.customDialog();
        }

        @Override
        protected FragmentActivity getActivity() {
            return BaseFragment.this.getActivity();
        }

        @Override
        protected Context getContext() {
            return BaseFragment.this.getContext();
        }

        @Override
        protected LifecycleOwner getLifecycleOwner() {
            return getViewLifecycleOwner();
        }

        @Override
        protected Lifecycle getBaseLifecycle() {
            return getLifecycle();
        }

        @Override
        protected LifecycleProvider getLifecycleProvider() {
            return BaseFragment.this;
        }

        @Override
        protected int initContentView(Bundle savedInstanceState) {
            return 0;
        }

        @Override
        protected void initParam() {

        }
    };

    protected V binding;
    private VM viewModel;

    protected VM getViewModel(){
        if (null == viewModel){
            viewModel = baseView.ensureViewModelCreated();
        }
        return viewModel;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParam();
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        menuVisibleTag = menuVisible;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, initContentView(inflater, container, savedInstanceState), container, false);
        baseView.binding = binding;
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        baseView.onDestroy();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        baseView.initialize(savedInstanceState);
    }

    public abstract void loadData();

    /**
     * 是否注册事件分发
     *
     * @return true绑定EventBus事件分发，默认不绑定，子类需要绑定的话复写此方法返回true.
     */
    protected boolean isRegisterEventBus() {
        return false;
    }

    public boolean customDialog(){
        return false;
    }

    public boolean mvvmDialog(){
        return false;
    }

    public void showDialog(){
        baseView.showDialog();
    }

    public void showDialog(String title){
        baseView.showDialog(title);
    }

    public void showCustomDialog(){}

    protected void requestCallPhone(boolean denied){}

    public void dismissCustomDialog(){
    }

    public void requestPermission(IPermission iPermission,String... permissions){
        baseView.requestPermission(iPermission, permissions);
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

    public void startActivityForResult(Class<?> clz,int requestCode, Bundle bundle) {
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

    @Override
    public void initParam() {

    }

    /**
     * 初始化根布局
     *
     * @return 布局layout的id
     */
    public abstract int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @Override
    public void initData() {

    }

    @Override
    public void initViewObservable() {

    }

    public void dismissDialog() {
        baseView.dismissDialog();
    }

    public boolean isBackPressed() {
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBusCome(MessageEvent event) {
        if (event != null) {
            viewModel.receiveEvent(event);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onStickyEventBusCome(MessageEvent event) {
        if (event != null) {
            viewModel.receiveStickyEvent(event);
        }
    }
}
