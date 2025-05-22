package com.ved.framework.base;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.blankj.swipepanel.SwipePanel;
import com.mumu.dialog.MMLoading;
import com.orhanobut.dialog.manager.DialogManager;
import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.R;
import com.ved.framework.bus.Messenger;
import com.ved.framework.bus.event.eventbus.EventBusUtil;
import com.ved.framework.entity.ParameterField;
import com.ved.framework.permission.IPermission;
import com.ved.framework.permission.RxPermission;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.DpiUtils;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.SoftKeyboardUtil;
import com.ved.framework.utils.phone.PhoneUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

abstract class BaseView<V extends ViewDataBinding, VM extends BaseViewModel> {
    protected V binding;
    protected volatile VM viewModel;
    private MMLoading mmLoading;

    protected final void initialize(Bundle savedInstanceState) {
        initParam();
        initViewDataBinding(savedInstanceState);
        registerUIChangeLiveDataCallBack();
    }

    protected void initViewDataBinding(Bundle savedInstanceState) {
        if (getLifecycleOwner() instanceof FragmentActivity) {
            binding = DataBindingUtil.setContentView(getActivity(), initContentView(savedInstanceState));
            getBinding(binding);
        }
        viewModel = initViewModel();
        if (null == viewModel) {
            viewModel = ensureViewModelCreated();
        }
        getViewModel(viewModel);
        if (binding != null && viewModel != null) {
            binding.setVariable(Constant.variableId, viewModel);
            binding.setLifecycleOwner(getLifecycleOwner());
            getBaseLifecycle().addObserver(viewModel);
            viewModel.injectLifecycleProvider(getLifecycleProvider());
        } else {
            KLog.e("Critical: Binding or ViewModel is null");
        }
    }

    protected VM ensureViewModelCreated() {
        if (null == viewModel) {
            synchronized (this) {
                if (null == viewModel) {
                    Class modelClass = resolveViewModelClass();
                    KLog.d("Creating ViewModel of type: " + modelClass.getName());

                    try {
                        // 1. 尝试标准方式创建
                        viewModel = (VM) createViewModelWithProvider(modelClass);

                        // 2. 回退到反射创建
                        if (viewModel == null) {
                            viewModel = createViewModelWithReflection(modelClass);
                        }

                        // 3. 终极回退方案
                        if (viewModel == null && !BaseViewModel.class.equals(modelClass)) {
                            KLog.w("Falling back to BaseViewModel");
                            viewModel = (VM) createViewModelWithProvider(BaseViewModel.class);
                        }

                        // 最终检查
                        if (viewModel == null) {
                            throw new IllegalStateException("Failed to create ViewModel after all attempts");
                        }

                        // 类型验证
                        if (!modelClass.isInstance(viewModel)) {
                            throw new ClassCastException("Created ViewModel is not of type " + modelClass.getName());
                        }
                    } catch (Exception e) {
                        KLog.e("ViewModel creation error: " + e.getMessage());
                        throw new RuntimeException("Failed to create ViewModel", e);
                    }
                }
            }
        }
        return viewModel;
    }

    private Class<?> resolveViewModelClass() {
        try {
            // 方法1：尝试通过泛型获取
            Type type = getLifecycleOwner().getClass().getGenericSuperclass();
            while (type != null) {
                if (type instanceof ParameterizedType) {
                    Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                    if (types.length > 1 && types[1] instanceof Class) {
                        return (Class<?>) types[1];
                    }
                }

                if (type instanceof Class) {
                    type = ((Class<?>) type).getGenericSuperclass();
                } else {
                    break;
                }
            }

            // 方法2：尝试通过注解获取
            ViewModelClass annotation = getLifecycleOwner().getClass().getAnnotation(ViewModelClass.class);
            if (annotation != null) {
                return annotation.value();
            }

            throw new IllegalStateException("Cannot determine ViewModel class");
        } catch (Exception e) {
            KLog.e("Failed to resolve ViewModel class: " + e.getMessage());
            throw new RuntimeException("Cannot determine ViewModel type", e);
        }
    }

    private <T extends ViewModel> T createViewModelWithProvider(Class<T> modelClass) {
        try {
            if (getLifecycleOwner() instanceof FragmentActivity) {
                return ViewModelProviders.of((FragmentActivity) getLifecycleOwner()).get(modelClass);
            } else if (getLifecycleOwner() instanceof Fragment) {
                return ViewModelProviders.of((Fragment) getLifecycleOwner()).get(modelClass);
            }
            return null;
        } catch (Exception e) {
            KLog.w("ViewModelProvider creation failed: " + e.getMessage());
            return null;
        }
    }

    private VM createViewModelWithReflection(Class<?> modelClass) {
        try {
            Constructor<?> constructor = modelClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (VM) constructor.newInstance();
        } catch (NoSuchMethodException e) {
            KLog.w("No default constructor for " + modelClass.getSimpleName());
        } catch (Exception e) {
            KLog.e("Reflective ViewModel creation failed: " + e.getMessage());
        }
        return null;
    }

    private void registerUIChangeLiveDataCallBack() {
        viewModel.getUC().getShowDialogEvent().observe(getLifecycleOwner(), (Observer<String>) this::showDialog);
        viewModel.getUC().getDismissDialogEvent().observe(getLifecycleOwner(), (Observer<Void>) v -> dismissDialog());

        viewModel.getUC().getRequestPermissionEvent().observe(getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            IPermission iPermission = (IPermission) params.get(Constant.PERMISSION);
            String[] permissions = (String[]) params.get(Constant.PERMISSION_NAME);
            requestPermission(iPermission, permissions);
        });

        viewModel.getUC().getRequestCallPhoneEvent().observe(getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            String phoneNumber = (String) params.get(Constant.PHONE_NUMBER);
            callPhone(phoneNumber);
        });

        viewModel.getUC().getStartActivityEvent().observe(getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startActivity(clz, bundle);
        });

        viewModel.getUC().getStartActivityForResultEvent().observe(getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            int requestCode = (int) params.get(ParameterField.REQUEST_CODE);
            startActivityForResult(clz, requestCode, bundle);
        });

        viewModel.getUC().getStartContainerActivityEvent().observe(getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
            String canonicalName = (String) params.get(ParameterField.CANONICAL_NAME);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startContainerActivity(canonicalName, bundle);
        });

        viewModel.getUC().getFinishEvent().observe(getLifecycleOwner(), (Observer<Void>) v -> {
            SoftKeyboardUtil.hideSoftKeyboard(getActivity());
            getActivity().finish();
        });

        viewModel.getUC().getOnBackPressedEvent().observe(getLifecycleOwner(), (Observer<Void>) v -> getActivity().onBackPressed());

        viewModel.getUC().getOnLoadEvent().observe(getLifecycleOwner(), o -> {
            if (getLifecycleOwner() instanceof FragmentActivity){
                initView();
                initSwipeBack();
            }
            if (isRegisterEventBus()) {
                EventBusUtil.register(this);
            }
            //页面事件监听的方法，一般用于ViewModel层转到View层的事件注册
            initViewObservable();
            //注册RxBus
            viewModel.registerRxBus();
        });

        viewModel.getUC().getReceiverEvent().observe(getLifecycleOwner(),o -> sendReceiver());

        if (viewModel.getUC().getOnResumeEvent() != null && getLifecycleOwner() instanceof Fragment) {
            viewModel.getUC().getOnResumeEvent().observe(getLifecycleOwner(),
                    o -> initView());
        }
    }

    protected void showDialog() {
        showDialog("加载中...");
    }

    protected void showDialog(String title){
        if (customDialog()) {
            showCustomDialog();
        } else {
            if (mvvmDialog()) {
                showDialog(title,true);
            } else {
                DialogManager.Companion.getInstance().showProgressDialog(getActivity(),title);
            }
        }
    }

    protected void showDialog(String title,boolean isShowMessage) {
        if (mmLoading == null) {
            MMLoading.Builder builder = new MMLoading.Builder(getActivity())
                    .setMessage(title)
                    .setShowMessage(isShowMessage)
                    .setCancelable(false)
                    .setCancelOutside(false);
            mmLoading = builder.create();
        }else {
            mmLoading.dismiss();
            MMLoading.Builder builder = new MMLoading.Builder(getActivity())
                    .setMessage(title)
                    .setShowMessage(isShowMessage)
                    .setCancelable(false)
                    .setCancelOutside(false);
            mmLoading = builder.create();
        }
        mmLoading.getWindow().setDimAmount(0f);
        mmLoading.show();
    }

    protected void dismissDialog() {
        if (customDialog()) {
            dismissCustomDialog();
        } else {
            if (mvvmDialog()) {
                if (mmLoading != null && mmLoading.isShowing()) {
                    mmLoading.dismiss();
                }
            }else {
                DialogManager.Companion.getInstance().dismiss();
            }
        }
    }

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    protected void startActivity(Class<?> clz) {
        getContext().startActivity(new Intent(getContext(), clz));
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    protected void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent(getContext(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        getContext().startActivity(intent);
    }

    protected void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        Intent intent = new Intent(getContext(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (getLifecycleOwner() instanceof FragmentActivity) {
            ((FragmentActivity) getLifecycleOwner()).startActivityForResult(intent, requestCode);
        } else if (getLifecycleOwner() instanceof Fragment) {
            ((Fragment) getLifecycleOwner()).startActivityForResult(intent, requestCode);
        }
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     */
    protected void startContainerActivity(String canonicalName) {
        startContainerActivity(canonicalName, null);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     * @param bundle        跳转所携带的信息
     */
    protected void startContainerActivity(String canonicalName, Bundle bundle) {
        Intent intent = new Intent(getContext(), ContainerActivity.class);
        intent.putExtra(ContainerActivity.FRAGMENT, canonicalName);
        if (bundle != null) {
            intent.putExtra(ContainerActivity.BUNDLE, bundle);
        }
        getContext().startActivity(intent);
    }

    public void callPhone(String phoneNumber) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermission(new IPermission() {
                @Override
                public void onGranted() {
                    PhoneUtils.callPhone(phoneNumber);
                }

                @Override
                public void onDenied(boolean denied) {
                    requestCallPhone(denied);
                }
            }, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE);
        } else {
            requestPermission(new IPermission() {
                @Override
                public void onGranted() {
                    PhoneUtils.callPhone(phoneNumber);
                }

                @Override
                public void onDenied(boolean denied) {
                    requestCallPhone(denied);
                }
            }, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE);
        }
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        if (getLifecycleOwner() instanceof FragmentActivity) {
            RxPermission.requestPermission((FragmentActivity) getLifecycleOwner(), iPermission, permissions);
        } else if (getLifecycleOwner() instanceof Fragment) {
            RxPermission.requestPermission((Fragment) getLifecycleOwner(), iPermission, permissions);
        }
    }

    /**
     * 发送广播
     */
    public void sendReceiver(Bundle bundle){
        Intent intent = new Intent(Constant.RECEIVER_ACTION);
        if (bundle != null){
            intent.putExtras(bundle);
        }
        getContext().sendBroadcast(intent);
    }

    public void sendReceiver(){
        sendReceiver(null);
    }

    private void initSwipeBack() {
        if (isSwipeBack()) {
            final SwipePanel swipeLayout = new SwipePanel(getActivity());
            swipeLayout.setLeftDrawable(R.drawable.ca);
            swipeLayout.setLeftEdgeSize(DpiUtils.dip2px(getActivity(),16));
            swipeLayout.setLeftSwipeColor(getActivity().getResources().getColor(R.color.colorPrimary));
            swipeLayout.wrapView(getActivity().findViewById(android.R.id.content));
            swipeLayout.setOnFullSwipeListener(direction -> {
                swipeLayout.close(direction);
                getActivity().finish();
            });
        }
    }

    protected void onDestroy() {
        try {
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
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
    }

    protected abstract boolean isSwipeBack();

    protected abstract void initViewObservable();

    protected abstract boolean isRegisterEventBus();

    protected abstract void initView();

    protected abstract void requestCallPhone(boolean denied);

    protected abstract void getBinding(V binding);

    protected abstract void getViewModel(VM viewModel);

    protected abstract VM initViewModel();

    protected abstract void dismissCustomDialog();

    protected abstract boolean mvvmDialog();

    protected abstract void showCustomDialog();

    protected abstract boolean customDialog();

    protected abstract FragmentActivity getActivity();

    protected abstract Context getContext();

    protected abstract LifecycleOwner getLifecycleOwner();

    protected abstract Lifecycle getBaseLifecycle();

    protected abstract LifecycleProvider getLifecycleProvider();

    protected abstract int initContentView(Bundle savedInstanceState);

    protected abstract void initParam();
}
