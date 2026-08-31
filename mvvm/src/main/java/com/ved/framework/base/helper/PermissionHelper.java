package com.ved.framework.base.helper;

import android.Manifest;
import android.os.Build;

import androidx.databinding.ViewDataBinding;

import com.orhanobut.dialog.utils.WifiSignalHelper;
import com.ved.framework.base.BaseViewModel;
import com.ved.framework.base.IBaseView;
import com.ved.framework.permission.IPermission;
import com.ved.framework.permission.RxPermission;
import com.ved.framework.utils.phone.PhoneUtils;

/**
 * 权限与系统能力助手（单一职责原则）：
 * 封装运行时权限申请、拨打电话、WiFi 信号强度监听等系统能力，
 * 使 BaseView 从系统权限细节中解耦。
 */
public class PermissionHelper<V extends ViewDataBinding, VM extends BaseViewModel> {

    private final IBaseView<V, VM> viewDelegate;

    public PermissionHelper(IBaseView<V, VM> viewDelegate) {
        this.viewDelegate = viewDelegate;
    }

    /** 申请运行时权限 */
    public void requestPermission(IPermission iPermission, String... permissions) {
        // getLifecycleOwner() 在 Fragment 场景返回 viewLifecycleOwner，instanceof 判断恒为 false，
        // 导致 Fragment 内请求权限静默失效，需用 viewDelegate 判断宿主类型
        if (viewDelegate.isFragment() && viewDelegate.getFragment() != null) {
            RxPermission.requestPermission(viewDelegate.getFragment(), iPermission, permissions);
        } else if (viewDelegate.FragmentActivity() != null) {
            RxPermission.requestPermission(viewDelegate.FragmentActivity(), iPermission, permissions);
        }
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
