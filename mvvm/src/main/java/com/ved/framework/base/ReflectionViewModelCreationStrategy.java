package com.ved.framework.base;

import java.lang.reflect.Constructor;

public class ReflectionViewModelCreationStrategy implements ViewModelCreationStrategy{
    @Override
    public <VM extends BaseViewModel> VM createViewModel(Class<VM> viewModelClass, Object owner) {
        try {
            Constructor<VM> constructor = viewModelClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
