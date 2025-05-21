package com.ved.framework.base;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ved on 2018/9/30.
 */

public class ViewModelFactory{
    private static final List<ViewModelCreationStrategy> strategies = Arrays.asList(
            new DefaultViewModelCreationStrategy(),
            new ReflectionViewModelCreationStrategy()
    );

    public static <VM extends BaseViewModel> VM create(Class<VM> viewModelClass, Object owner) {
        for (ViewModelCreationStrategy strategy : strategies) {
            VM viewModel = strategy.createViewModel(viewModelClass, owner);
            if (viewModel != null) {
                return viewModel;
            }
        }
        return null;
    }
}
