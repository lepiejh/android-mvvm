package com.ved.framework.base;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ViewModelProxyImpl<VM extends BaseViewModel> implements ViewModelProxy<VM> {
    private final Object obj;
    private VM viewModel;

    public ViewModelProxyImpl(Object obj) {
        this.obj = obj;
    }

    public <T extends ViewModel> T createViewModel(Object obj, Class<T> cls) {
        if (obj instanceof FragmentActivity){
            // 修复：默认工厂无法创建继承 AndroidViewModel 的 BaseViewModel（需要 Application 构造参数），
            // 改用 AndroidViewModelFactory
            return new ViewModelProvider((FragmentActivity) obj, createFactory((FragmentActivity) obj)).get(cls);
        }else if (obj instanceof Fragment){
            Fragment fragment = (Fragment) obj;
            if (fragment.getActivity() != null) {
                return new ViewModelProvider(fragment, createFactory(fragment.getActivity())).get(cls);
            }
        }
        return null;
    }

    private ViewModelProvider.Factory createFactory(FragmentActivity activity) {
        return ViewModelProvider.AndroidViewModelFactory.getInstance(activity.getApplication());
    }

    @Override
    public VM createViewModel() {
        if (viewModel == null) {
            Class modelClass;
            Type type = obj.getClass().getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                modelClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[1];
            } else {
                //如果没有指定泛型参数，则默认使用BaseViewModel
                modelClass = BaseViewModel.class;
            }
            viewModel = (VM) createViewModel(obj, modelClass);
            return viewModel;
        }
        return viewModel;
    }
}