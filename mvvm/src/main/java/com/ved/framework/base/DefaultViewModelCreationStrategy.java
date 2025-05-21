package com.ved.framework.base;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

public class DefaultViewModelCreationStrategy implements ViewModelCreationStrategy{
    @Override
    public <VM extends BaseViewModel> VM createViewModel(Class<VM> viewModelClass, Object owner) {
        if (owner instanceof Fragment) {
            return ViewModelProviders.of((Fragment) owner).get(viewModelClass);
        } else if (owner instanceof FragmentActivity) {
            return ViewModelProviders.of((FragmentActivity) owner).get(viewModelClass);
        }
        return null;
    }
}
