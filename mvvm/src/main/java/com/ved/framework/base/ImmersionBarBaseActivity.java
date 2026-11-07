package com.ved.framework.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity;
import com.ved.framework.R;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.ScreenUtils;
import com.ved.framework.utils.bland.code.ObjectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class ImmersionBarBaseActivity extends RxAppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener{
    private ImmersionBar mImmersionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //为了解决Android自level 1以来的[安装完成点击“Open”后导致的应用被重复启动]的Bug
        if (!isTaskRoot()){
            Intent intent = getIntent();
            String intentAction = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && ObjectUtils.equals(intentAction,Intent.ACTION_MAIN)){
                finish();
            }
        }
        // Android P 以上无需处理（这里原本用于 hook SystemHandler，已移除）
        // Android 8.0 的透明主题 Activity 固定方向会触发系统检查导致闪退，需要反射绕开
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && isTranslucentOrFloating()) {
            boolean result = fixOrientation();
            KLog.i("only_opaque", "result : " + result);
        }
        super.onCreate(savedInstanceState);
        if (isRegular()) {
            if (!ScreenUtils.isTabletDevice()){
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        }
        initStatusBar();
    }

    public boolean isRegular(){
        return true;
    }

    //通过反射判断是否是透明页面
    @SuppressLint("PrivateApi")
    private boolean isTranslucentOrFloating() {
        boolean isTranslucentOrFloating = false;
        TypedArray ta = null;
        try {
            int[] styleableRes = (int[]) Class.forName("com.android.internal.R$styleable").getField("Window").get(null);
            if (styleableRes == null) return isTranslucentOrFloating;
            ta = obtainStyledAttributes(styleableRes);
            String methodName = "isTranslucentOrFloating";
            Method m = ActivityInfo.class.getMethod(methodName, TypedArray.class);
            m.setAccessible(true);
            Boolean isTranslucent = (Boolean) m.invoke(null, ta);
            isTranslucentOrFloating = isTranslucent != null && isTranslucent;
            m.setAccessible(false);
        } catch (Exception e) {
            KLog.e(e.getMessage());
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
        return isTranslucentOrFloating;
    }

    //通过反射将方向设置为 SCREEN_ORIENTATION_UNSPECIFIED，绕开系统的检查
    private boolean fixOrientation() {
        try {
            String methodName = "mActivityInfo";
            @SuppressLint("DiscouragedPrivateApi") Field field = Activity.class.getDeclaredField(methodName);
            field.setAccessible(true);
            ActivityInfo o = (ActivityInfo) field.get(this);
            if (o != null) {
                o.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            }
            field.setAccessible(false);
            return true;
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return false;
    }

    /**
     * 获取状态栏字体颜色
     */
    public boolean statusBarDarkFont() {
        //返回false表示白色字体
        return true;
    }

    public int statusBarColor(){
        return R.color.colorPrimary;
    }

    //是否支持手势拦截  状态栏和导航栏
    public boolean isStickyImmersiveEnabled(){
        return true;
    }

    public void initStatusBar() {
        //初始化沉浸式状态栏
        if (isStatusBarEnabled()) {
            statusBarConfig().init();

            if (statusBarHideMode().needsStickyFullScreen() && isStickyImmersiveEnabled()){
                // 设置全屏模式，防止手势滑动时显示状态栏和导航栏
                setupFullScreenImmersiveMode();
            }
        }
    }

    /**
     * 设置完全沉浸式全屏模式
     * 防止从底部上滑或顶部下滑时显示状态栏和导航栏
     */
    private void setupFullScreenImmersiveMode() {
        View decorView = getWindow().getDecorView();

        // 使用 androidx 的 WindowInsetsControllerCompat 替代已过时的 View#setSystemUiVisibility
        // 与 Window#setDecorFitsSystemWindows，一套 API 兼容 Android 11 上下的全屏沉浸式
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), decorView);
        // 用户从边缘滑动时短暂显示系统栏后自动重新隐藏，等价于旧的 SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insetsController.hide(WindowInsetsCompat.Type.systemBars());

        // 设置窗口标志，确保全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && statusBarHideMode().needsStickyFullScreen() && isStickyImmersiveEnabled()) {
            // 每次窗口获得焦点时重新应用全屏模式
            setupFullScreenImmersiveMode();
            if (mImmersionBar != null) {
                mImmersionBar.hideBar(BarHide.FLAG_HIDE_BAR).init();
            }
        }
    }

    /**
     * 是否使用沉浸式状态栏
     */
    public boolean isStatusBarEnabled() {
        return true;
    }

    public boolean statusBarColorDef(){
        return true;
    }

    /**
     * 设置状态栏背景颜色
     */
    public void setStatusBarColor(int color){
        if (mImmersionBar != null && statusBarColorDef()) {
            mImmersionBar.statusBarColor(color).init();
        }
    }

    /**
     * 设置状态栏字体的颜色
     */
    public void setStatusBarDarkFont(boolean blackFont){
        if (mImmersionBar != null) {
            mImmersionBar.statusBarDarkFont(blackFont).init();
        }
    }

    /**
     * 初始化沉浸式状态栏
     */
    private ImmersionBar statusBarConfig() {
        //状态栏沉浸
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.statusBarDarkFont(statusBarDarkFont());   //默认状态栏字体颜色为黑色
        if (statusBarColorDef()) {
            mImmersionBar.statusBarColor(statusBarColor());
        }
        // 隐藏策略由模式枚举内聚（原 0/1/2/3 类型码 switch 已收敛）
        statusBarHideMode().applyTo(mImmersionBar);
        mImmersionBar.keyboardEnable(false, WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);  //解决软键盘与底部输入框冲突问题，默认为false，还有一个重载方法，可以指定软键盘mode
        //必须设置View树布局变化监听，否则软键盘无法顶上去，还有模式必须是SOFT_INPUT_ADJUST_PAN
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        return mImmersionBar;
    }

    /**
     * 状态栏/导航栏隐藏模式（策略入口）。
     * <p>默认转发旧类型码钩子 {@link #getStatusBarHide()}：
     * 覆盖了旧钩子的既有子类行为不变；新子类建议直接覆盖本方法返回枚举。
     */
    public StatusBarHideMode statusBarHideMode() {
        return StatusBarHideMode.fromLegacyCode(getStatusBarHide());
    }

    /**
     * 旧类型码钩子：0=隐藏状态栏，1=隐藏导航栏，2=隐藏全部，其他=显示全部。
     * <p>新代码建议覆盖 {@link #statusBarHideMode()}；本钩子保留以保证既有子类兼容。
     */
    public int getStatusBarHide(){
        return 3;
    }

    /**
     * 系统栏隐藏模式枚举（策略模式）：每个常量内聚自己的 ImmersionBar 应用行为
     * 与「是否需要 sticky 全屏」判定，取代原 0/1/2/3 类型码 switch 分派。
     */
    public enum StatusBarHideMode {
        /** 显示状态栏和导航栏 */
        SHOW_BAR,
        /** 隐藏状态栏 */
        HIDE_STATUS_BAR,
        /** 隐藏导航栏（启用全屏模式） */
        HIDE_NAVIGATION_BAR,
        /** 隐藏状态栏和导航栏 */
        HIDE_BOTH;

        /** 旧类型码 → 枚举映射 */
        static StatusBarHideMode fromLegacyCode(int code) {
            switch (code) {
                case 0:
                    return HIDE_STATUS_BAR;
                case 1:
                    return HIDE_NAVIGATION_BAR;
                case 2:
                    return HIDE_BOTH;
                default:
                    return SHOW_BAR;
            }
        }

        /** 将本模式应用到 ImmersionBar 配置 */
        void applyTo(ImmersionBar bar) {
            switch (this) {
                case HIDE_STATUS_BAR:
                    bar.hideBar(BarHide.FLAG_HIDE_STATUS_BAR);
                    break;
                case HIDE_NAVIGATION_BAR:
                    bar.hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR);
                    bar.fullScreen(true);  // 启用全屏模式
                    break;
                case HIDE_BOTH:
                    bar.hideBar(BarHide.FLAG_HIDE_BAR);
                    break;
                default:
                    bar.hideBar(BarHide.FLAG_SHOW_BAR);
                    break;
            }
        }

        /** 隐藏全部时需要配合 sticky 全屏，防止手势滑动时系统栏回显 */
        boolean needsStickyFullScreen() {
            return this == HIDE_BOTH;
        }
    }

    @Override
    public void onGlobalLayout() {

    }

    @Override
    protected void onDestroy() {
        getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
        super.onDestroy();
    }
}
