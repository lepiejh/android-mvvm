package com.ved.framework.base;

import androidx.databinding.ViewDataBinding;

/**
 * 视图统一入口接口（接口隔离原则）：
 * 将宿主环境、视图行为、特性开关三类职责拆分为角色接口，
 * 全部行为提供默认实现，子类只覆写需要的方法。
 */
public interface IBaseView<V extends ViewDataBinding, VM extends BaseViewModel>
        extends IViewHost<V, VM>, IViewAction, IViewFeature {
}
