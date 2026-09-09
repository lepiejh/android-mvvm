package com.ved.framework.base;

/**
 * 视图特性开关接口（接口隔离原则）：
 * 集中定义页面可选特性的开关，默认关闭，
 * 子类按需开启，避免每个基类重复覆写返回 false 的样板方法。
 */
public interface IViewFeature {

    /** 是否启用滑动返回 */
    default boolean isSwipeBack() {
        return false;
    }

    /** 是否注册 EventBus */
    default boolean isRegisterEventBus() {
        return false;
    }

    /** 是否监听 WiFi 信号强度 */
    default boolean hasWifi() {
        return false;
    }

    /** 是否使用 MVVM 对话框 */
    default boolean mvvmDialog() {
        return false;
    }

    /** 是否使用自定义对话框 */
    default boolean customDialog() {
        return false;
    }

    /** 页面可见时是否需要重新加载数据 */
    default boolean needReload() {
        return false;
    }
}
