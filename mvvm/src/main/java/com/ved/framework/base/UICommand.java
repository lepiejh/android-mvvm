package com.ved.framework.base;

import android.os.Bundle;

import com.orhanobut.dialog.navigation.ActivityCommandBuilder;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.Constant;

import java.util.HashMap;
import java.util.Map;

public class UICommand implements ICommand {
    private final UIChangeLiveData liveData = new UIChangeLiveData();

    public void showDialog() {
        showDialog("请稍后...");
    }

    public void showDialog(String title) {
        liveData.getShowDialogEvent().postValue(title);
    }

    public void dismissDialog() {
        liveData.getDismissDialogEvent().call();
    }

    public void startActivity(Class<?> clz) {
        startActivity(clz, null);
    }

    public void startActivity(Class<?> clz, Bundle bundle) {
        newActivityBuilder(bundle)
                .setTarget(clz)
                .execute(liveData.getStartActivityEvent());
    }

    public void startActivityForResult(Class<?> clz, int requestCode) {
        startActivityForResult(clz, null, requestCode);
    }

    public void startActivityForResult(Class<?> clz, Bundle bundle, int requestCode) {
        newActivityBuilder(bundle)
                .setTarget(clz)
                .setRequestCode(requestCode)
                .execute(liveData.getStartActivityForResultEvent());
    }

    public void startContainerActivity(String canonicalName) {
        startContainerActivity(canonicalName, null);
    }

    public void startContainerActivity(String canonicalName, Bundle bundle) {
        newActivityBuilder(bundle)
                .setCanonicalName(canonicalName)
                .execute(liveData.getStartContainerActivityEvent());
    }

    /**
     * 统一创建 Activity 跳转 Builder，消除三个 start 方法中重复的创建+参数注入逻辑
     */
    private ActivityCommandBuilder newActivityBuilder(Bundle bundle) {
        return ActivityCommandBuilder.create().setBundle(bundle);
    }

    /**
     * 构建参数 Map（可变参数版），消除每个命令方法中重复的 Map 创建样板代码
     */
    private static Map<String, Object> params(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    public void requestPermissions(IPermission iPermission, String... permissions) {
        liveData.getRequestPermissionEvent().postValue(
                params(Constant.PERMISSION, iPermission, Constant.PERMISSION_NAME, permissions));
    }

    public void callPhone(String phoneNumber) {
        liveData.getRequestCallPhoneEvent().postValue(params(Constant.PHONE_NUMBER, phoneNumber));
    }

    public void getWifiRssi(){
        liveData.getRequestWifiRssiEvent().call();
    }

    public void sendReceiver() {
        sendReceiver(null);
    }

    public void sendReceiver(Bundle bundle) {
        liveData.getReceiverEvent().postValue(bundle);
    }

    public void finish() {
        liveData.getFinishEvent().call();
    }

    public void onBackPressed() {
        liveData.getOnBackPressedEvent().call();
    }

    public UIChangeLiveData getLiveData() {
        return liveData;
    }
}
