package com.ved.framework.base;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

/**
 * 页面工厂接口（工厂模式）：
 * 定义 Fragment 的创建契约，将"创建什么"与"如何创建"解耦，
 * 便于替换不同的创建策略（反射、依赖注入等）。
 */
public interface FragmentFactory {

    /**
     * 根据规范名创建 Fragment 实例
     *
     * @param canonicalName Fragment 类规范名，如 Fragment.class.getCanonicalName()
     * @param args          需要注入 Fragment 的参数（可空）
     * @return Fragment 实例，创建失败时抛出异常
     */
    Fragment create(String canonicalName, Bundle args);
}
