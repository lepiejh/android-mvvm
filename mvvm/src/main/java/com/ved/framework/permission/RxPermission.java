package com.ved.framework.permission;

import android.content.pm.PackageManager;
import android.os.Build;

import com.tbruyelle.rxpermissions3.RxPermissions;

import java.util.Arrays;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * 权限请求组件（模板方法 + 宿主归一化）：
 * <p>
 * 原实现里 FragmentActivity / Fragment 两条 22 行的同构订阅链逐字复制，
 * 仅强转目标不同；现收敛为「解析宿主 Activity → 统一请求管道」两步，
 * 新增宿主类型只需扩展 {@link #resolveHostActivity}，订阅链只有这一份实现。
 */
public class RxPermission {

    public static void requestPermission(final Object object, final IPermission iPermission, final String... permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            iPermission.onGranted();
            return;
        }
        final FragmentActivity activity = resolveHostActivity(object);
        if (activity == null) {
            // 无法解析出宿主（非 FragmentActivity/Fragment，或 Fragment 已 detach），
            // 无法挂载权限请求，静默忽略——原实现在此场景会 NPE，这里顺势加固
            return;
        }
        if (hasAlwaysDeniedPermission(activity, Arrays.asList(permission))) {
            iPermission.onGranted();
            return;
        }
        // 宿主适配：RxPermissions 的 Fragment/FragmentActivity 两种构造器内部都会
        // 归一到宿主 FragmentActivity 上挂载无 UI Fragment，统一用 Activity 构造
        final RxPermissions rxPermissions = new RxPermissions(activity);
        rxPermissions.request(permission).subscribe(granted -> {
            if (granted) {
                iPermission.onGranted();
            } else {
                // 授权失败时查询「是否勾选了不再询问」：
                // rationale 为 true（还会弹系统框）→ 用户只是本次拒绝；
                // rationale 为 false（不再弹）→ 视为永久拒绝
                rxPermissions.shouldShowRequestPermissionRationale(activity, permission)
                        .subscribe(shouldShowRationale -> iPermission.onDenied(!shouldShowRationale));
            }
        });
    }

    /**
     * 宿主归一化：Fragment 场景取其挂载的 FragmentActivity，Activity 原样返回。
     * 与原 Fragment 分支中 rationale 查询传 {@code fragment.getActivity()} 的取值一致。
     */
    private static FragmentActivity resolveHostActivity(Object object) {
        if (object instanceof FragmentActivity) {
            return (FragmentActivity) object;
        }
        if (object instanceof Fragment) {
            return ((Fragment) object).getActivity();
        }
        return null;
    }

    private static boolean hasAlwaysDeniedPermission(FragmentActivity activity, List<String> asList) {
        for (String permission : asList) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
