package com.ved.framework.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

public abstract class BaseActivity<V extends ViewDataBinding, VM extends BaseViewModel> extends ImmersionBarBaseActivity implements IBaseView{
    private final BaseView<V, VM> baseView = new BaseView<V, VM>() {

        @Override
        protected int initContentView(Bundle savedInstanceState) {
            return BaseActivity.this.initContentView(savedInstanceState);
        }

        @Override
        protected VM ensureViewModelCreated() {
            return BaseActivity.this.ensureViewModelCreated();
        }

        @Override
        protected boolean isSwipeBack() {
            return BaseActivity.this.isSwipeBack();
        }

        @Override
        protected void initViewObservable() {
            BaseActivity.this.initViewObservable();
        }

        @Override
        protected boolean isRegisterEventBus() {
            return BaseActivity.this.isRegisterEventBus();
        }

        @Override
        protected void initView() {
            //页面数据初始化方法
            BaseActivity.this.initData();
        }

        @Override
        protected void requestCallPhone(boolean denied) {
            BaseActivity.this.requestCallPhone(denied);
        }

        @Override
        protected void getBinding(V binding) {
            BaseActivity.this.binding = binding;
        }

        @Override
        protected void dismissCustomDialog() {
            BaseActivity.this.dismissCustomDialog();
        }

        @Override
        protected boolean mvvmDialog() {
            return BaseActivity.this.mvvmDialog();
        }

        @Override
        protected void showCustomDialog() {
            BaseActivity.this.showCustomDialog();
        }

        @Override
        protected boolean customDialog() {
            return BaseActivity.this.customDialog();
        }

        @Override
        protected FragmentActivity getActivity() {
            return BaseActivity.this;
        }

        @Override
        protected Context getContext() {
            return BaseActivity.this;
        }

        @Override
        protected LifecycleOwner getLifecycleOwner() {
            return BaseActivity.this;
        }

        @Override
        protected Lifecycle getBaseLifecycle() {
            return BaseActivity.this.getLifecycle();
        }

        @Override
        protected LifecycleProvider getLifecycleProvider() {
            return BaseActivity.this;
        }
    };

    protected V binding;
    private VM viewModel;

    protected VM getViewModel(){
        if (null == viewModel){
            viewModel = ensureViewModelCreated();
        }
        return viewModel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseView.initialize(savedInstanceState);
    }

    public <T extends ViewModel> T createViewModel(FragmentActivity fragmentActivity, Class<T> cls) {
        return ViewModelProviders.of(fragmentActivity).get(cls);
    }

    /**
     * 如果放到BaseView里面可能获取不到viewModel
     */
    private VM ensureViewModelCreated(){
        Class modelClass;
        Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            modelClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[1];
        } else {
            //如果没有指定泛型参数，则默认使用BaseViewModel
            modelClass = BaseViewModel.class;
        }
        viewModel = (VM) createViewModel(this, modelClass);
        return viewModel;
    }

    public boolean isSwipeBack() {
        return false;
    }

    public boolean customDialog(){
        return false;
    }

    public boolean mvvmDialog(){
        return false;
    }

    /**
     * 是否注册事件分发
     *
     * @return true绑定EventBus事件分发，默认不绑定，子类需要绑定的话复写此方法返回true.
     */
    protected boolean isRegisterEventBus() {
        return false;
    }

    /**
     * 是否注册广播
     */
    protected boolean isReceiver(){
        return false;
    }

    /**
     * 接收广播
     */
    public void onReceive(Intent intent){
    }

    @Override
    protected void onDestroy() {
        KLog.i(this.getLocalClassName()+" : onDestroy()");
        super.onDestroy();
        baseView.onDestroy();
    }

    protected void requestCallPhone(boolean denied){}

    public void showDialog(){
        baseView.showDialog();
    }

    public void showDialog(String title){
        baseView.showDialog(title);
    }

    public void showCustomDialog(){}

    public void dismissDialog() {
        baseView.dismissDialog();
    }

    public void dismissCustomDialog(){}

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

    /**
     * =====================================================================
     **/
    @Override
    public void initParam() {

    }

    /**
     * 初始化根布局
     *
     * @return 布局layout的id
     */
    public abstract int initContentView(Bundle savedInstanceState);

    @Override
    public void initData() {

    }

    @Override
    public void initViewObservable() {

    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        baseView.requestPermission(iPermission, permissions);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBusCome(MessageEvent event) {
        if (event != null && viewModel != null) {
            viewModel.receiveEvent(event);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onStickyEventBusCome(MessageEvent event) {
        if (event != null && viewModel != null) {
            viewModel.receiveStickyEvent(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        KLog.i(this.getLocalClassName()+" : onResume()");
    }

    @Override
    protected void onPause() {
        KLog.i(this.getLocalClassName()+" : onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        KLog.i(this.getLocalClassName()+" : onStop()");
        super.onStop();
    }
}
