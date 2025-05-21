package com.ved.framework.base;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mumu.dialog.MMLoading;
import com.orhanobut.dialog.manager.DialogManager;
import com.trello.rxlifecycle4.LifecycleProvider;
import com.trello.rxlifecycle4.components.support.RxFragment;
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

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

public abstract class BaseFragment<V extends ViewDataBinding, VM extends BaseViewModel> extends RxFragment implements IBaseView {

    protected boolean menuVisibleTag =false;
    protected boolean isLoadData = false;

    private final BaseView<V, VM> baseView = new BaseView<V, VM>() {
        @Override
        protected void getBinding(V binding) {

        }

        @Override
        protected void getViewModel(VM viewModel) {
            BaseFragment.this.viewModel = viewModel;
        }

        @Override
        protected VM initViewModel() {
            return BaseFragment.this.initViewModel();
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
        protected Activity getActivity() {
            return BaseFragment.this.getActivity();
        }

        @Override
        protected LifecycleOwner getLifecycleOwner() {
            return BaseFragment.this;
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
        protected void registerUIChangeLiveDataCallBack() {
            BaseFragment.this.registorUIChangeLiveDataCallBack();
        }

        @Override
        protected int initContentView(Bundle savedInstanceState) {
            return 0;
        }

        @Override
        protected void initParam() {

        }

        @Override
        protected int initVariableId() {
            return Constant.variableId;
        }
    };

    protected V binding;
    protected VM viewModel;

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

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRegisterEventBus()) {
            EventBusUtil.unregister(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, initContentView(inflater, container, savedInstanceState), container, false);
        baseView.binding = binding;
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //解除Messenger注册
        Messenger.getDefault().unregister(viewModel);
        if (viewModel != null) {
            viewModel.removeRxBus();
        }
        if (binding != null) {
            binding.unbind();
        }
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

    /**
     * =====================================================================
     **/
    //注册ViewModel与View的契约UI回调事件
    protected void registorUIChangeLiveDataCallBack() {
        //加载对话框显示
        viewModel.getUC().getShowDialogEvent().observe(getViewLifecycleOwner(), (Observer<String>) title -> showDialog(title));
        //加载对话框消失
        viewModel.getUC().getDismissDialogEvent().observe(getViewLifecycleOwner(), (Observer<Void>) v -> dismissDialog());
        viewModel.getUC().getRequestPermissionEvent().observe(getViewLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            IPermission iPermission = (IPermission) params.get(Constant.PERMISSION);
            String[] permissions = (String[]) params.get(Constant.PERMISSION_NAME);
            requestPermission(iPermission,permissions);
        });
        viewModel.getUC().getRequestCallPhoneEvent().observe(getViewLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            String phoneNumber = (String) params.get(Constant.PHONE_NUMBER);
            callPhone(phoneNumber);
        });
        //跳入新页面
        viewModel.getUC().getStartActivityEvent().observe(getViewLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startActivity(clz, bundle);
        });
        viewModel.getUC().getStartActivityForResultEvent().observe(getViewLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            int requestCode = (int) params.get(ParameterField.REQUEST_CODE);
            startActivityForResult(clz,requestCode, bundle);
        });
        //跳入ContainerActivity
        viewModel.getUC().getStartContainerActivityEvent().observe(getViewLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            String canonicalName = (String) params.get(ParameterField.CANONICAL_NAME);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startContainerActivity(canonicalName, bundle);
        });
        //关闭界面
        viewModel.getUC().getFinishEvent().observe(getViewLifecycleOwner(), (Observer<Void>) v -> getActivity().finish());
        //关闭上一层
        viewModel.getUC().getOnBackPressedEvent().observe(getViewLifecycleOwner(), (Observer<Void>) v -> getActivity().onBackPressed());
        viewModel.getUC().getOnLoadEvent().observe(getViewLifecycleOwner(), o -> {
            if (isRegisterEventBus()) {
                EventBusUtil.register(this);
            }
            //页面事件监听的方法，一般用于ViewModel层转到View层的事件注册
            initViewObservable();
            //注册RxBus
            viewModel.registerRxBus();
        });
        viewModel.getUC().getOnResumeEvent().observe(getViewLifecycleOwner(), o -> {
            if (menuVisibleTag && !isLoadData) {
                isLoadData = true;
                //页面数据初始化方法
                initData();
                loadData();
            }
        });
    }

    public void showDialog(){
        baseView.showDialog();
    }

    public void showDialog(String title){
        baseView.showDialog(title);
    }

    public void showCustomDialog(){}

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

    public void dismissCustomDialog(){
    }

    public void requestPermission(IPermission iPermission,String... permissions){
        RxPermission.requestPermission(this,iPermission,permissions);
    }

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    public void startActivity(Class<?> clz) {
        startActivity(new Intent(getContext(), clz));
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent(getContext(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    public void startActivityForResult(Class<?> clz,int requestCode, Bundle bundle) {
        Intent intent = new Intent(getContext(), clz);
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
        Intent intent = new Intent(getContext(), ContainerActivity.class);
        intent.putExtra(ContainerActivity.FRAGMENT, canonicalName);
        if (bundle != null) {
            intent.putExtra(ContainerActivity.BUNDLE, bundle);
        }
        startActivity(intent);
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
