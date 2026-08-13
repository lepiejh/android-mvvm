package com.ved.framework.base;

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

    public static Stack<Activity> getActivityStack() {
        return activityStack;
    }

    public static Stack<Fragment> getFragmentStack() {
        return fragmentStack;
    }

    // ===================== 泛型模板方法（Activity / Fragment 共用） =====================

    /**
     * 惰性初始化栈（模板步骤 1）
     */
    private static <T> Stack<T> ensure(Stack<T> stack) {
        return stack != null ? stack : new Stack<>();
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
        for (Activity activity : activityStack) {
            if (activity.getClass().equals(cls)) {
                finishActivity(activity);
                break;
            }
        }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
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
        if (activityStack != null)
            for (Activity activity : activityStack) {
                if (activity.getClass().equals(cls)) {
                    return activity;
                }
            }
        return null;
    }

    // ===================== Fragment 栈操作 =====================

    /**
     * 添加Fragment到堆栈
     */
    public void addFragment(@Nullable Fragment fragment) {
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

    /**
     * 退出应用程序
     */
    public void AppExit() {
        try {
            finishAllActivity();
            // 杀死该应用进程
//          android.os.Process.killProcess(android.os.Process.myPid());
//            调用 System.exit(n) 实际上等效于调用：
//            Runtime.getRuntime().exit(n)
//            finish()是Activity的类方法，仅仅针对Activity，当调用finish()时，只是将活动推向后台，并没有立即释放内存，活动的资源并没有被清理；当调用System.exit(0)时，退出当前Activity并释放资源（内存），但是该方法不可以结束整个App如有多个Activty或者其他组件service等不会结束。
//            其实android的机制决定了用户无法完全退出应用，当你的application最长时间没有被用过的时候，android自身会决定将application关闭了。
            //System.exit(0);
        } catch (Exception e) {
            activityStack.clear();
            e.printStackTrace();
        }
    }
}
