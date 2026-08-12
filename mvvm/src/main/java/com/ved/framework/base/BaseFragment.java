package com.ved.framework.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.trello.rxlifecycle4.LifecycleProvider;
import com.trello.rxlifecycle4.android.FragmentEvent;
import com.trello.rxlifecycle4.components.support.RxFragment;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseFragment<V extends ViewDataBinding, VM extends BaseViewModel> extends RxFragment implements IBaseView<V, VM> {

    protected boolean menuVisibleTag = false;
    protected boolean isLoadData = false;
    private final BaseView<V, VM> baseView = new BaseView<>(this);

    protected V binding;
    private final ViewModelDelegate<VM> viewModelDelegate = new ViewModelDelegate<>(this);

    protected VM getViewModel() {
        return viewModelDelegate.getViewModel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        initParam();
        super.onCreate(savedInstanceState);
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

    @Override
    public VM ensureViewModelCreated() {
        return viewModelDelegate.ensureViewModelCreated();
    }

    @Override
    public boolean needReload() {
        return true;
    }

    @Override
    public void initView() {
        if (needReload()){
            KLog.i("BaseFragment menuVisibleTag ："+menuVisibleTag+", isLoadData : "+isLoadData);
            if (menuVisibleTag && !isLoadData) {
                isLoadData = true;
                refreshView();
            }
        }else {
            refreshView();
        }
    }

    @Override
    public void refreshView(){
        //页面数据初始化方法
        BaseFragment.this.initData();
        BaseFragment.this.loadData();
    }

    @Override
    public V getBinding(Bundle savedInstanceState) {
        return binding;
    }

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
        return BaseFragment.this;
    }

    public void loadData(){

    }

    public void showDialog() {
        baseView.showDialog();
    }

    public void showDialog(String title) {
        baseView.showDialog(title);
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
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

    /**
     * 初始化根布局
     *
     * @return 布局layout的id
     */
    public abstract int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public void dismissDialog() {
        baseView.dismissDialog();
    }

    public boolean isBackPressed() {
        return false;
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
}
