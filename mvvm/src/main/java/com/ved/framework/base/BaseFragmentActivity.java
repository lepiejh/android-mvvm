package com.ved.framework.base;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.mumu.dialog.MMLoading;
import com.orhanobut.dialog.manager.DialogManager;
import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.R;
import com.ved.framework.bus.Messenger;
import com.ved.framework.bus.event.eventbus.EventBusUtil;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.entity.ParameterField;
import com.ved.framework.permission.IPermission;
import com.ved.framework.permission.RxPermission;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.phone.PhoneUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

public abstract class BaseFragmentActivity<V extends ViewDataBinding, VM extends BaseViewModel> extends RxAppCompatFragmentActivity implements IBaseView, ViewTreeObserver.OnGlobalLayoutListener{
    private final BaseView<V, VM> baseView = new BaseView<V, VM>() {
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
        protected Activity getContext() {
            return BaseFragmentActivity.this;
        }

        @Override
        protected LifecycleOwner getLifecycleOwner() {
            return BaseFragmentActivity.this;
        }

        @Override
        protected Lifecycle getLifecycle() {
            return getLifecycle();
        }

        @Override
        protected LifecycleProvider getLifecycleProvider() {
            return BaseFragmentActivity.this;
        }

        @Override
        protected void registerUIChangeLiveDataCallBack() {
            BaseFragmentActivity.this.registorUIChangeLiveDataCallBack();
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

    protected V binding = baseView.binding;
    private ImmersionBar mImmersionBar;
    protected volatile VM viewModel = baseView.viewModel;

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
        //解除Messenger注册
        Messenger.getDefault().unregister(viewModel);
        if (viewModel != null) {
            viewModel.removeRxBus();
        }
        if(binding != null){
            binding.unbind();
        }
        if (isRegisterEventBus()) {
            EventBusUtil.unregister(this);
        }
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
     * =====================================================================
     **/
    //注册ViewModel与View的契约UI回调事件
    protected void registorUIChangeLiveDataCallBack() {
        //加载对话框显示
        viewModel.getUC().getShowDialogEvent().observe(this, (Observer<String>) this::showDialog);
        //加载对话框消失
        viewModel.getUC().getDismissDialogEvent().observe(this, (Observer<Void>) v -> baseView.dismissDialog());
        //跳入新页面
        viewModel.getUC().getStartActivityEvent().observe(this, (Observer<Map<String, Object>>) params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startActivity(clz, bundle);
        });
        viewModel.getUC().getStartActivityForResultEvent().observe(this, (Observer<Map<String, Object>>) params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            int requestCode = (int) params.get(ParameterField.REQUEST_CODE);
            startActivityForResult(clz,requestCode, bundle);
        });
        viewModel.getUC().getRequestCallPhoneEvent().observe(this, (Observer<Map<String, Object>>) params -> {
            String phoneNumber = (String) params.get(Constant.PHONE_NUMBER);
            callPhone(phoneNumber);
        });
        viewModel.getUC().getRequestPermissionEvent().observe(this, (Observer<Map<String, Object>>) params -> {
            IPermission iPermission = (IPermission) params.get(Constant.PERMISSION);
            String[] permissions = (String[]) params.get(Constant.PERMISSION_NAME);
            requestPermission(iPermission,permissions);
        });
        //跳入ContainerActivity
        viewModel.getUC().getStartContainerActivityEvent().observe(this, (Observer<Map<String, Object>>) params -> {
            String canonicalName = (String) params.get(ParameterField.CANONICAL_NAME);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startContainerActivity(canonicalName, bundle);
        });
        //关闭界面
        viewModel.getUC().getFinishEvent().observe(this, (Observer<Void>) v -> finish());
        //关闭上一层
        viewModel.getUC().getOnBackPressedEvent().observe(this, (Observer<Void>) v -> onBackPressed());
        viewModel.getUC().getOnLoadEvent().observe(this, o -> {
            initStatusBar();
            //页面数据初始化方法
            initData();
            if (isRegisterEventBus()) {
                EventBusUtil.register(this);
            }
            //页面事件监听的方法，一般用于ViewModel层转到View层的事件注册
            initViewObservable();
            //注册RxBus
            viewModel.registerRxBus();
        });
    }

    public void requestPermission(IPermission iPermission,String... permissions){
        RxPermission.requestPermission(this,iPermission,permissions);
    }

    public void callPhone(String phoneNumber){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            requestPermission(new IPermission() {
                @Override
                public void onGranted() {
                    PhoneUtils.callPhone(phoneNumber);
                }

                @Override
                public void onDenied(boolean denied) {
                    requestCallPhone(denied);
                }
            }, Manifest.permission.READ_PHONE_STATE,Manifest.permission.CALL_PHONE);
        }else{
            requestPermission(new IPermission() {
                @Override
                public void onGranted() {
                    PhoneUtils.callPhone(phoneNumber);
                }

                @Override
                public void onDenied(boolean denied) {
                    requestCallPhone(denied);
                }
            },Manifest.permission.READ_PHONE_STATE,Manifest.permission.CALL_PHONE);
        }
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
        startActivity(new Intent(this, clz));
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent(this, clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }
    public void startActivityForResult(Class<?> clz,int requestCode, Bundle bundle) {
        Intent intent = new Intent(this, clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent,requestCode);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     */
    public void startContainerActivity(String canonicalName) {
        startContainerActivity(canonicalName, null);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     * @param bundle        跳转所携带的信息
     */
    public void startContainerActivity(String canonicalName, Bundle bundle) {
        Intent intent = new Intent(this, ContainerActivity.class);
        intent.putExtra(ContainerActivity.FRAGMENT, canonicalName);
        if (bundle != null) {
            intent.putExtra(ContainerActivity.BUNDLE, bundle);
        }
        startActivity(intent);
    }

    /**
     * =====================================================================
     **/
    @Override
    public void initParam() {

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

    protected VM ensureViewModelCreated() {
        if (viewModel == null) {
            synchronized (this) { // 同步锁防止多线程重复创建
                if (viewModel == null) { // 双重检查锁定
                    Class<?> modelClass = resolveViewModelClass();
                    viewModel = createViewModelSafely(modelClass);

                    // 终极回退方案
                    if (viewModel == null) {
                        viewModel = (VM) createViewModel(this,BaseViewModel.class);
                        KLog.w("Using fallback BaseViewModel");
                    }

                    if (viewModel == null) {
                        throw new IllegalStateException("ViewModel creation failed after all attempts");
                    }
                }
            }
        }
        return viewModel;
    }

    private Class<?> resolveViewModelClass() {
        try {
            // 方法1：尝试通过泛型获取
            Type type = getClass().getGenericSuperclass();
            while (!(type instanceof ParameterizedType) && type != null && type instanceof Class) {
                type = ((Class<?>) type).getGenericSuperclass();
            }

            if (type instanceof ParameterizedType) {
                Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                if (types.length > 1) {
                    Type actualType = types[1];
                    if (actualType instanceof Class) {
                        return (Class<?>) actualType;
                    } else if (actualType instanceof ParameterizedType) {
                        return (Class<?>) ((ParameterizedType) actualType).getRawType();
                    }
                }
            }

            // 方法2：尝试通过注解获取（备用方案）
            ViewModelClass annotation = getClass().getAnnotation(ViewModelClass.class);
            if (annotation != null) {
                return annotation.value();
            }

            // 方法3：使用默认 BaseViewModel
            return BaseViewModel.class;
        } catch (Exception e) {
            KLog.e("Failed to resolve ViewModel class: " + e.getMessage());
            return BaseViewModel.class;
        }
    }

    private VM createViewModelSafely(Class modelClass) {
        try {
            // 尝试标准方式创建
            ViewModel viewModel = createViewModel(this, modelClass);
            if (viewModel != null) {
                return (VM) viewModel;
            }

            // 尝试反射创建（备用方案）
            try {
                Constructor<?> constructor = modelClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return (VM) constructor.newInstance();
            } catch (NoSuchMethodException e) {
                KLog.w("No default constructor for " + modelClass.getSimpleName());
            }

        } catch (Exception e) {
            KLog.e("Failed to create ViewModel: " + e.getMessage());
        }
        return null;
    }

    public <T extends ViewModel> T createViewModel(FragmentActivity activity, Class<T> cls) {
        return ViewModelProviders.of(activity).get(cls);
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
