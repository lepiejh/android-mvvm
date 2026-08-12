package com.ved.framework.base;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

/**
 * 基于反射的页面工厂（工厂模式）：
 * 通过类规范名反射创建 Fragment 实例并注入参数，
 * 使用 {@code getDeclaredConstructor().newInstance()} 替代已废弃的 {@code newInstance()}。
 */
public class ReflectFragmentFactory implements FragmentFactory {

    @Override
    public Fragment create(String canonicalName, Bundle args) {
        try {
            Class<?> fragmentClass = Class.forName(canonicalName);
            Fragment fragment = (Fragment) fragmentClass.getDeclaredConstructor().newInstance();
            if (args != null) {
                fragment.setArguments(args);
            }
            return fragment;
        } catch (Exception e) {
            throw new RuntimeException("fragment initialization failed!", e);
        }
    }
}
