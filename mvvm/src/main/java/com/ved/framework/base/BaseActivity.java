package com.ved.framework.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.blankj.swipepanel.SwipePanel;
import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.R;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.DpiUtils;
import com.ved.framework.utils.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

public abstract class BaseActivity<V extends ViewDataBinding, VM extends BaseViewModel> extends ImmersionBarBaseActivity implements IBaseView{
    private final BaseView<V, VM> baseView = new BaseView<V, VM>() {

        @Override
        protected int initContentView(Bundle savedInstanceState) {
            return BaseActivity.this.initContentView(savedInstanceState);
        }

        @Override
        protected void initParam() {
            BaseActivity.this.initParam();
        }

        @Override
        protected int initVariableId() {
            return Constant.variableId;
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
            BaseActivity.this.initSwipeBack();
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
        protected void getViewModel(VM viewModel) {
            BaseActivity.this.viewModel = viewModel;
        }

        @Override
        protected VM initViewModel() {
            return BaseActivity.this.initViewModel();
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
        protected Activity getActivity() {
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
    protected VM viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseView.initialize(savedInstanceState);
    }

    private void initSwipeBack() {
        if (isSwipeBack()) {
            final SwipePanel swipeLayout = new SwipePanel(this);
            swipeLayout.setLeftDrawable(R.drawable.ca);
            swipeLayout.setLeftEdgeSize(DpiUtils.dip2px(BaseActivity.this,16));
            swipeLayout.setLeftSwipeColor(getResources().getColor(R.color.colorPrimary));
            swipeLayout.wrapView(findViewById(android.R.id.content));
            swipeLayout.setOnFullSwipeListener(direction -> {
                swipeLayout.close(direction);
                finish();
            });
        }
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

    /**
     * 初始化ViewModel
     *
     * @return 继承BaseViewModel的ViewModel
     */
    public VM initViewModel() {
        return null;
    }

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
