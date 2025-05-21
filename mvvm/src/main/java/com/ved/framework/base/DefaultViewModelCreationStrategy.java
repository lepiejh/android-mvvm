package com.ved.framework.base;

import com.ved.framework.utils.KLog;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

public class DefaultViewModelCreationStrategy implements ViewModelCreationStrategy{
    @Override
    public <VM extends BaseViewModel> VM createViewModel(Class<VM> viewModelClass, Object owner) {
        try {
            // 使用 ViewModelProvider 确保正确创建
            if (owner instanceof FragmentActivity) {
                return (VM) new ViewModelProvider((FragmentActivity) owner).get(viewModelClass);
            } else if (owner instanceof Fragment) {
                return (VM) new ViewModelProvider((Fragment) owner).get(viewModelClass);
            }

            throw new IllegalStateException("Invalid owner for ViewModel creation");
        } catch (Exception e) {
            KLog.e("Failed to create ViewModel: " + e.getMessage());
            throw new RuntimeException("ViewModel creation failed", e);
        }
    }
}
