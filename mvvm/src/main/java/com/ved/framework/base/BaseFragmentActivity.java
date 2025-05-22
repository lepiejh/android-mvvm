package com.ved.framework.base;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.R;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

public abstract class BaseFragmentActivity<V extends ViewDataBinding, VM extends BaseViewModel> extends RxAppCompatFragmentActivity implements IBaseView, ViewTreeObserver.OnGlobalLayoutListener{
    private final BaseView<V, VM> baseView = new BaseView<V, VM>() {

        @Override
        protected boolean isSwipeBack() {
            return BaseFragmentActivity.this.isSwipeBack();
        }

        @Override
        protected void initViewObservable() {
            BaseFragmentActivity.this.initViewObservable();
        }

        @Override
        protected boolean isRegisterEventBus() {
            return BaseFragmentActivity.this.isRegisterEventBus();
        }

        @Override
        protected void initView() {
            BaseFragmentActivity.this.initStatusBar();
            //页面数据初始化方法
            BaseFragmentActivity.this.initData();
        }

        @Override
        protected void requestCallPhone(boolean denied) {
            BaseFragmentActivity.this.requestCallPhone(denied);
        }

        @Override
        protected void getBinding(V binding) {
            BaseFragmentActivity.this.binding = binding;
        }

        @Override
        protected void getViewModel(VM viewModel) {
            BaseFragmentActivity.this.viewModel = viewModel;
        }

        @Override
        protected VM initViewModel() {
            return BaseFragmentActivity.this.initViewModel();
        }

        @Override
        protected void dismissCustomDialog() {
            BaseFragmentActivity.this.dismissCustomDialog();
        }

        @Override
        protected boolean mvvmDialog() {
            return BaseFragmentActivity.this.mvvmDialog();
        }

        @Override
        protected void showCustomDialog() {
            BaseFragmentActivity.this.showCustomDialog();
        }

        @Override
        protected boolean customDialog() {
            return BaseFragmentActivity.this.customDialog();
        }

        @Override
        protected FragmentActivity getActivity() {
            return BaseFragmentActivity.this;
        }

        @Override
        protected Context getContext() {
            return BaseFragmentActivity.this;
        }

        @Override
        protected LifecycleOwner getLifecycleOwner() {
            return BaseFragmentActivity.this;
        }

        @Override
        protected Lifecycle getBaseLifecycle() {
            return getLifecycle();
        }

        @Override
        protected LifecycleProvider getLifecycleProvider() {
            return BaseFragmentActivity.this;
        }

        @Override
        protected int initContentView(Bundle savedInstanceState) {
            return BaseFragmentActivity.this.initContentView(savedInstanceState);
        }

        @Override
        protected void initParam() {
            BaseFragmentActivity.this.initParam();
        }

        @Override
        protected int initVariableId() {
            return Constant.variableId;
        }
    };

    protected V binding;
    private ImmersionBar mImmersionBar;
    protected VM viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseView.initialize(savedInstanceState);
    }

    public void initStatusBar() {
        //初始化沉浸式状态栏
        if (isStatusBarEnabled()) {
            statusBarConfig().init();
        }
    }

    /**
     * 是否使用沉浸式状态栏
     */
    public boolean isStatusBarEnabled() {
        return true;
    }

    /**
     * 设置状态栏背景颜色
     */
    public void setStatusBarColor(int color){
        if (statusBarColorDef()) {
            mImmersionBar.statusBarColor(color).init();
        }
    }

    /**
     * 设置状态栏字体的颜色
     */
    public void setStatusBarDarkFont(boolean blackFont){
        mImmersionBar.statusBarDarkFont(blackFont).init();
    }

    /**
     * 初始化沉浸式状态栏
     */
    private ImmersionBar statusBarConfig() {
        //    private MaterialDialog dialog;
        //状态栏沉浸
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.statusBarDarkFont(statusBarDarkFont());   //默认状态栏字体颜色为黑色
        if (statusBarColorDef()) {
            mImmersionBar.statusBarColor(statusBarColor());
        }
        switch (getStatusBarHide()){
            case 0 :
                //隐藏状态栏
                mImmersionBar.hideBar(BarHide.FLAG_HIDE_STATUS_BAR);
                break;
            case 1 :
                //隐藏导航栏
                mImmersionBar.hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR);
                break;
            case 2 :
                //隐藏状态栏和导航栏
                mImmersionBar.hideBar(BarHide.FLAG_HIDE_BAR);
                break;
            default:
                //显示状态栏和导航栏
                mImmersionBar.hideBar(BarHide.FLAG_SHOW_BAR);
                break;
        }
        mImmersionBar.keyboardEnable(false, WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);  //解决软键盘与底部输入框冲突问题，默认为false，还有一个重载方法，可以指定软键盘mode
        //必须设置View树布局变化监听，否则软键盘无法顶上去，还有模式必须是SOFT_INPUT_ADJUST_PAN
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        return mImmersionBar;
    }

    public boolean statusBarColorDef(){
        return true;
    }

    public int statusBarColor(){
        return R.color.colorPrimary;
    }

    public int getStatusBarHide(){
        return 3;
    }

    /**
     * 获取状态栏字体颜色
     */
    public boolean statusBarDarkFont() {
        //返回false表示白色字体
        return true;
    }

    @Override
    public void onGlobalLayout() {

    }

    @Override
    protected void onDestroy() {
        KLog.i(this.getLocalClassName()+" : onDestroy()");
        super.onDestroy();
        baseView.onDestroy();
    }

    /**
     * 是否注册事件分发
     *
     * @return true绑定EventBus事件分发，默认不绑定，子类需要绑定的话复写此方法返回true.
     */
    protected boolean isRegisterEventBus() {
        return false;
    }

    public void requestPermission(IPermission iPermission,String... permissions){
        baseView.requestPermission(iPermission, permissions);
    }

    protected void requestCallPhone(boolean denied){}

    public void showDialog(){
        baseView.showDialog();
    }

    public void showDialog(String title){
        baseView.showDialog(title);
    }

    public void showCustomDialog(){}

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
     * 初始化根布局
     *
     * @return 布局layout的id
     */
    public abstract int initContentView(Bundle savedInstanceState);

    /**
     * 初始化ViewModel的id
     *
     * @return BR的id
     */
    public int initVariableId(){
        return Constant.variableId;
    }

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

    public void dismissDialog() {
        baseView.dismissDialog();
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
