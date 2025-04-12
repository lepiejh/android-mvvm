package com.ved.framework.base;

import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.ved.framework.R;

public class ImmersionBarBaseActivity extends RxAppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener{
    private ImmersionBar mImmersionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStatusBar();
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

    public void initStatusBar() {
        //初始化沉浸式状态栏
        if (isStatusBarEnabled()) {
            statusBarConfig().init();
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
        if (statusBarColorDef()) {
            mImmersionBar.statusBarColor(color).init();
        }
    }

    /**
     * 设置状态栏字体的颜色
     */
    public void setStatusBarDarkFont(boolean blackFont){
        mImmersionBar.statusBarDarkFont(blackFont).init();
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
        switch (getStatusBarHide()){
            case 0 :
                //隐藏状态栏
                mImmersionBar.hideBar(BarHide.FLAG_HIDE_STATUS_BAR);
                break;
            case 1 :
                //隐藏导航栏
                mImmersionBar.hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR);
                break;
            case 2 :
                //隐藏状态栏和导航栏
                mImmersionBar.hideBar(BarHide.FLAG_HIDE_BAR);
                break;
            default:
                //显示状态栏和导航栏
                mImmersionBar.hideBar(BarHide.FLAG_SHOW_BAR);
                break;
        }
        mImmersionBar.keyboardEnable(false, WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);  //解决软键盘与底部输入框冲突问题，默认为false，还有一个重载方法，可以指定软键盘mode
        //必须设置View树布局变化监听，否则软键盘无法顶上去，还有模式必须是SOFT_INPUT_ADJUST_PAN
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        return mImmersionBar;
    }

    public int getStatusBarHide(){
        return 3;
    }

    @Override
    public void onGlobalLayout() {

    }
}
