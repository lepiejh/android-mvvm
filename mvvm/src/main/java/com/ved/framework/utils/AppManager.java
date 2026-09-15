package com.ved.framework.utils;

import android.app.Activity;

import java.util.Stack;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Created by ved on 2017/6/15.
 * activity堆栈式管理
 * <p>
 * 模板方法 + 泛型：Activity 栈与 Fragment 栈的对称操作（add / remove / isEmpty / current）
 * 收敛为私有泛型模板方法，公共 API 保持不变。
 * <p>
 * 修复：activityStack / fragmentStack 是静态字段，初始为 null。
 * 所有会触碰集合的公共方法（finishAllActivity / getActivityStack / getFragmentStack /
 * isActivity / isFragment 等）均需判空，避免在"还没有任何 Activity 入栈"时调用直接 NPE。
 */
public class AppManager {

    private static Stack<Activity> activityStack;
    private static Stack<Fragment> fragmentStack;
    private static AppManager instance;

    private AppManager() {
    }

    /**
     * 单例模式
     *
     * @return AppManager
     */
    public static AppManager getAppManager() {
        if (instance == null) {
            instance = new AppManager();
        }
        return instance;
    }

    /**
     * 获取 Activity 栈。
     * <p>
     * 注意：为兼容旧调用方，返回值可能为 null（尚未有任何 Activity 入栈时）。
     * 调用方使用前必须判空。
     */
    public static Stack<Activity> getActivityStack() {
        return activityStack;
    }

    /**
     * 获取 Fragment 栈。
     * <p>
     * 注意：为兼容旧调用方，返回值可能为 null（尚未有任何 Fragment 入栈时）。
     * 调用方使用前必须判空。
     */
    public static Stack<Fragment> getFragmentStack() {
        return fragmentStack;
    }

    // ===================== 泛型模板方法（Activity / Fragment 共用） =====================

    /**
     * 惰性初始化栈（模板步骤 1）
     */
    private static <T> Stack<T> ensure(Stack<T> stack) {
        return stack != null ? stack : new Stack<T>();
    }

    /**
     * 入栈（模板步骤 2）
     */
    private static <T> void push(Stack<T> stack, T item) {
        stack.add(item);
    }

    /**
     * 出栈（移除指定元素，模板步骤 3）
     */
    private static <T> void remove(Stack<T> stack, T item) {
        if (stack != null && item != null) {
            stack.remove(item);
        }
    }

    /**
     * 栈是否非空（模板步骤 4）
     */
    private static <T> boolean isNotEmpty(Stack<T> stack) {
        return stack != null && !stack.isEmpty();
    }

    /**
     * 取栈顶元素（模板步骤 5），空栈返回 null
     */
    private static <T> T current(Stack<T> stack) {
        return isNotEmpty(stack) ? stack.peek() : null;
    }

    // ===================== Activity 栈操作 =====================

    /**
     * 添加Activity到堆栈
     */
    public void addActivity(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        activityStack = ensure(activityStack);
        push(activityStack, activity);
    }

    /**
     * 移除指定的Activity
     */
    public void removeActivity(@Nullable Activity activity) {
        remove(activityStack, activity);
    }

    /**
     * 是否有activity
     */
    public boolean isActivity() {
        return isNotEmpty(activityStack);
    }

    /**
     * 获取当前Activity（堆栈中最后一个压入的）
     */
    public Activity currentActivity() {
        return current(activityStack);
    }

    /**
     * 结束当前Activity（堆栈中最后一个压入的）
     */
    public void finishActivity() {
        Activity activity = current(activityStack);
        finishActivity(activity);
    }

    /**
     * 结束指定的Activity
     */
    public void finishActivity(@Nullable Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            activity.finish();
        }
    }

    /**
     * 结束指定类名的Activity
     */
    public void finishActivity(@Nullable Class<?> cls) {
        if (cls == null || activityStack == null) {
            return;
        }
        for (Activity activity : activityStack) {
            if (activity != null && activity.getClass().equals(cls)) {
                finishActivity(activity);
                break;
            }
        }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
        // activityStack 尚未初始化（还没有任何 Activity 入栈）时直接返回，
        // 否则 activityStack.size() 会抛 NullPointerException
        if (activityStack == null) {
            return;
        }
        for (int i = 0, size = activityStack.size(); i < size; i++) {
            if (null != activityStack.get(i)) {
                finishActivity(activityStack.get(i));
            }
        }
        activityStack.clear();
    }

    /**
     * 获取指定的Activity
     *
     * @author kymjs
     */
    public Activity getActivity(@Nullable Class<?> cls) {
        if (activityStack != null && cls != null) {
            for (Activity activity : activityStack) {
                if (activity != null && activity.getClass().equals(cls)) {
                    return activity;
                }
            }
        }
        return null;
    }

    // ===================== Fragment 栈操作 =====================

    /**
     * 添加Fragment到堆栈
     */
    public void addFragment(@Nullable Fragment fragment) {
        if (fragment == null) {
            return;
        }
        fragmentStack = ensure(fragmentStack);
        push(fragmentStack, fragment);
    }

    /**
     * 移除指定的Fragment
     */
    public void removeFragment(@Nullable Fragment fragment) {
        remove(fragmentStack, fragment);
    }

    /**
     * 是否有Fragment
     */
    public boolean isFragment() {
        return isNotEmpty(fragmentStack);
    }

    /**
     * 获取当前Fragment（堆栈中最后一个压入的）
     */
    public Fragment currentFragment() {
        return current(fragmentStack);
    }
}