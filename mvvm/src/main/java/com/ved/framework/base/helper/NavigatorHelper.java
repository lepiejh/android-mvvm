package com.ved.framework.base.helper;

import android.content.Intent;
import android.os.Bundle;

import androidx.databinding.ViewDataBinding;

import com.orhanobut.dialog.navigation.ActivityNavigator;
import com.ved.framework.base.BaseViewModel;
import com.ved.framework.base.ContainerActivity;
import com.ved.framework.base.IBaseView;
import com.ved.framework.entity.ParameterField;

/**
 * 页面导航助手（单一职责原则）：
 * 封装页面跳转、带参跳转、容器页跳转等导航能力，
 * 使 BaseView 从导航细节中解耦。
 */
public class NavigatorHelper<V extends ViewDataBinding, VM extends BaseViewModel> {

    private final IBaseView<V, VM> viewDelegate;

    public NavigatorHelper(IBaseView<V, VM> viewDelegate) {
        this.viewDelegate = viewDelegate;
    }

    /** 跳转页面（无参数） */
    public void startActivity(Class<?> clz) {
        startActivity(clz, null);
    }

    /** 跳转页面（携带参数） */
    public void startActivity(Class<?> clz, Bundle bundle) {
        ActivityNavigator.with(viewDelegate.getViewContext())
                .target(clz)
                .bundle(bundle)
                .navigate();
    }

    /** 跳转页面并等待返回 */
    public void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        Intent intent = new Intent(viewDelegate.getViewContext(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        // 修复：getLifecycleOwner() 在 Fragment 场景返回 viewLifecycleOwner，instanceof 判断恒为 false，
        // 需用 viewDelegate.isFragment()/getFragment()/FragmentActivity() 判断宿主类型
        if (viewDelegate.isFragment() && viewDelegate.getFragment() != null) {
            viewDelegate.getFragment().startActivityForResult(intent, requestCode);
        } else if (viewDelegate.FragmentActivity() != null) {
            viewDelegate.FragmentActivity().startActivityForResult(intent, requestCode);
        }
    }

    /** 跳转容器页面（无参数） */
    public void startContainerActivity(String canonicalName) {
        startContainerActivity(canonicalName, null);
    }

    /** 跳转容器页面（携带参数） */
    public void startContainerActivity(String canonicalName, Bundle bundle) {
        ActivityNavigator.with(viewDelegate.getViewContext())
                .target(ContainerActivity.class)
                .putExtra(ParameterField.FRAGMENT, canonicalName)
                .bundle(bundle)
                .navigate();
    }
}
