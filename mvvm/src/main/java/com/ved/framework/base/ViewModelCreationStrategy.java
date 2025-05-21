package com.ved.framework.base;

public interface ViewModelCreationStrategy {
    <VM extends BaseViewModel> VM createViewModel(Class<VM> viewModelClass, Object owner);
}
