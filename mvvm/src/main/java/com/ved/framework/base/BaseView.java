package com.ved.framework.base;

import android.app.Activity;
import android.os.Bundle;

import com.mumu.dialog.MMLoading;
import com.orhanobut.dialog.manager.DialogManager;
import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.utils.KLog;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

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
            binding = DataBindingUtil.setContentView(getContext(), initContentView(savedInstanceState));
            getBinding(binding);
        }
        int viewModelId = initVariableId();
        viewModel = initViewModel();
        if (viewModel == null) {
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
        if (viewModel == null) {
            synchronized (this) {
                if (viewModel == null) {
                    Class modelClass = resolveViewModelClass();
                    viewModel = (VM) ViewModelFactory.create(modelClass, getLifecycleOwner());

                    if (viewModel == null) {
                        KLog.e("Failed to create ViewModel");
                        viewModel = (VM) ViewModelFactory.create(BaseViewModel.class, getLifecycleOwner());
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
                DialogManager.Companion.getInstance().showProgressDialog(getContext(),title);
            }
        }
    }

    public void showDialog(String title,boolean isShowMessage) {
        if (mmLoading == null) {
            MMLoading.Builder builder = new MMLoading.Builder(getContext())
                    .setMessage(title)
                    .setShowMessage(isShowMessage)
                    .setCancelable(false)
                    .setCancelOutside(false);
            mmLoading = builder.create();
        }else {
            mmLoading.dismiss();
            MMLoading.Builder builder = new MMLoading.Builder(getContext())
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

    protected abstract Activity getContext();

    protected abstract LifecycleOwner getLifecycleOwner();

    protected abstract Lifecycle getLifecycle();

    protected abstract LifecycleProvider getLifecycleProvider();

    protected abstract void registerUIChangeLiveDataCallBack();

    protected abstract int initContentView(Bundle savedInstanceState);

    protected abstract void initParam();

    protected abstract int initVariableId();
}
