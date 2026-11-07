package com.ved.framework.base;

import android.Manifest;
import android.os.Build;

import androidx.databinding.ViewDataBinding;

import com.orhanobut.dialog.utils.WifiSignalHelper;
import com.ved.framework.permission.IPermission;
import com.ved.framework.permission.RxPermission;
import com.ved.framework.utils.phone.PhoneUtils;

/**
 * 权限与系统能力助手（单一职责原则）：
 * 封装运行时权限申请、拨打电话、WiFi 信号强度监听等系统能力，
 * 使 BaseView 从系统权限细节中解耦。
 */
class PermissionHelper<V extends ViewDataBinding, VM extends BaseViewModel<?>> {

    private final IBaseView<V, VM> viewDelegate;

    public PermissionHelper(IBaseView<V, VM> viewDelegate) {
        this.viewDelegate = viewDelegate;
    }

    /** 申请运行时权限 */
    public void requestPermission(IPermission iPermission, String... permissions) {
        // 宿主双分派：Fragment 场景 getLifecycleOwner() 返回 viewLifecycleOwner，
        // instanceof 判断恒为 false，会导致权限申请静默失效，必须按宿主类型区分
        viewDelegate.dispatchToHost(
                fragment -> RxPermission.requestPermission(fragment, iPermission, permissions),
                activity -> RxPermission.requestPermission(activity, iPermission, permissions));
    }

    /** 拨打电话（自动申请权限） */
    public void callPhone(String phoneNumber) {
        requestPermission(new IPermission() {
            @Override
            public void onGranted() {
                PhoneUtils.callPhone(phoneNumber);
            }

            @Override
            public void onDenied(boolean denied) {
                viewDelegate.requestCallPhone(denied);
            }
        }, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE);
    }

    /** 获取当前 WiFi 信号强度（自动申请定位权限） */
    public void getWifiRssi() {
        if (!viewDelegate.hasWifi()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermission(new IPermission() {
                @Override
                public void onGranted() {
                    startListening();
                }

                @Override
                public void onDenied(boolean denied) {
                    viewDelegate.getWifiRssi(-100);
                }
            }, Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startListening();
        }
    }

    /** 停止 WiFi 信号监听 */
    public void stopWifiListening() {
        WifiSignalHelper.Companion.getINSTANCE().stopListening();
    }

    private void startListening() {
        WifiSignalHelper.Companion.getINSTANCE().startListening(i -> {
            viewDelegate.getWifiRssi(i);
            return null;
        });
    }
}
