package com.ved.framework.base;

/**
 * ViewModel 创建委托组件（委托模式 + 组合优于继承）：
 * 封装 ViewModel 的懒加载创建与缓存逻辑，
 * 供 BaseActivity / BaseFragment / BaseDialogFragment 复用，消除三处重复实现。
 */
public class ViewModelDelegate<VM extends BaseViewModel> {

    private final Object owner;
    private final ViewModelProxy<VM> viewModelProxy;
    private VM viewModel;

    public ViewModelDelegate(Object owner) {
        this.owner = owner;
        this.viewModelProxy = new ViewModelProxyImpl<>(owner);
    }

    /** 懒加载获取 ViewModel（已创建则直接返回缓存） */
    public VM getViewModel() {
        if (null == viewModel) {
            viewModel = viewModelProxy.createViewModel();
        }
        return viewModel;
    }

    /** 强制创建 ViewModel 并返回 */
    public VM ensureViewModelCreated() {
        viewModel = viewModelProxy.createViewModel();
        return viewModel;
    }

    /** 判断 ViewModel 是否已创建 */
    public boolean hasViewModel() {
        return viewModel != null;
    }

    /** 获取已创建的 ViewModel（未创建时返回 null） */
    public VM getCreatedViewModel() {
        return viewModel;
    }
}
