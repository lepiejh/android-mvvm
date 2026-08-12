package com.ved.framework.base;

/**
 * 视图行为角色接口（接口隔离原则）：
 * 定义页面业务生命周期回调与自定义行为，
 * 全部提供默认空实现，子类只需覆写需要的回调，减少冗余样板代码。
 */
public interface IViewAction {

    /** 初始化界面传递参数 */
    default void initParam() {
    }

    /** 初始化数据 */
    default void initData() {
    }

    /** 初始化界面观察者的监听 */
    default void initViewObservable() {
    }

    /** 初始化界面视图 */
    default void initView() {
        initData();
    }

    /** 刷新视图 */
    default void refreshView() {
    }

    /** 加载视图 */
    default void loadView() {
    }

    /** 请求拨打电话（denied 表示授权被拒绝） */
    default void requestCallPhone(boolean denied) {
    }

    /** 获取 WiFi 信号强度回调 */
    default void getWifiRssi(int rssi) {
    }

    /** 显示自定义对话框 */
    default void showCustomDialog() {
    }

    /** 关闭自定义对话框 */
    default void dismissCustomDialog() {
    }
}
