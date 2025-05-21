package com.ved.framework.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.mumu.dialog.MMLoading;
import com.orhanobut.dialog.manager.DialogManager;
import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.utils.KLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

public abstract class BaseView<V extends ViewDataBinding, VM extends BaseViewModel> {
    protected V binding;
    protected volatile VM viewModel;
    private MMLoading mmLoading;

    public final void initialize(Bundle savedInstanceState) {
        initParam();
        initViewDataBinding(savedInstanceState);
        registerUIChangeLiveDataCallBack();
    }

    protected void initViewDataBinding(Bundle savedInstanceState) {
        if (getLifecycleOwner() instanceof FragmentActivity) {
            binding = DataBindingUtil.setContentView(getActivity(), initContentView(savedInstanceState));
            getBinding(binding);
        }
        int viewModelId = initVariableId();
        viewModel = initViewModel();
        if (null == viewModel) {
            viewModel = ensureViewModelCreated();
        }
        getViewModel(viewModel);
        if (binding != null && viewModel != null) {
            binding.setVariable(viewModelId, viewModel);
            binding.setLifecycleOwner(getLifecycleOwner());
            getLifecycle().addObserver(viewModel);
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

    public void showDialog() {
        showDialog("加载中...");
    }

    public void showDialog(String title){
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

    public void showDialog(String title,boolean isShowMessage) {
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

    public void dismissDialog() {
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

    protected abstract void getBinding(V binding);

    protected abstract void getViewModel(VM viewModel);

    protected abstract VM initViewModel();

    protected abstract void dismissCustomDialog();

    protected abstract boolean mvvmDialog();

    protected abstract void showCustomDialog();

    protected abstract boolean customDialog();

    protected abstract Activity getActivity();

    protected abstract LifecycleOwner getLifecycleOwner();

    protected abstract Lifecycle getLifecycle();

    protected abstract LifecycleProvider getLifecycleProvider();

    protected abstract void registerUIChangeLiveDataCallBack();

    protected abstract int initContentView(Bundle savedInstanceState);

    protected abstract void initParam();

    protected abstract int initVariableId();
}
