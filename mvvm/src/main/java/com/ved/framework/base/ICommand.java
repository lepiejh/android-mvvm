package com.ved.framework.base;

import android.os.Bundle;

import com.ved.framework.permission.IPermission;

/**
 * UI 命令门面接口（门面模式）：
 * 定义 ViewModel 驱动 UI 变更的标准命令集合，
 * 页面通过 {@link BaseViewModel} 提供的门面方法间接调用，
 * 解耦命令的发起方（ViewModel）与执行方（View）。
 */
public interface ICommand {

    /** 显示加载对话框 */
    void showDialog();

    /** 显示指定标题的加载对话框 */
    void showDialog(String title);

    /** 关闭加载对话框 */
    void dismissDialog();

    /** 跳转页面（无参数） */
    void startActivity(Class<?> clz);

    /** 跳转页面（携带参数） */
    void startActivity(Class<?> clz, Bundle bundle);

    /** 跳转页面并等待返回（无参数） */
    void startActivityForResult(Class<?> clz, int requestCode);

    /** 跳转页面并等待返回（携带参数） */
    void startActivityForResult(Class<?> clz, Bundle bundle, int requestCode);

    /** 跳转容器页面（无参数） */
    void startContainerActivity(String canonicalName);

    /** 跳转容器页面（携带参数） */
    void startContainerActivity(String canonicalName, Bundle bundle);

    /** 请求运行时权限 */
    void requestPermissions(IPermission iPermission, String... permissions);

    /** 拨打电话 */
    void callPhone(String phoneNumber);

    /** 获取当前 WiFi 信号强度 */
    void getWifiRssi();

    /** 发送广播（无参数） */
    void sendReceiver();

    /** 发送广播（携带参数） */
    void sendReceiver(Bundle bundle);

    /** 结束当前页面 */
    void finish();

    /** 触发返回键事件 */
    void onBackPressed();

    /** 获取 UI 事件载体 */
    UIChangeLiveData getLiveData();
}
