package com.ved.framework.binding.viewadapter.view;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding4.view.RxView;
import com.ved.framework.binding.command.BindingCommand;
import com.ved.framework.binding.utils.ViewBindingUtils;
import com.ved.framework.listener.OnViewGlobalLayoutListener;
import com.ved.framework.utils.CalendarUtil;
import com.ved.framework.utils.CorpseUtils;
import com.ved.framework.utils.DpiUtils;
import com.ved.framework.utils.StringUtils;
import com.ved.framework.utils.TimeUtils;

import java.util.concurrent.TimeUnit;

import androidx.databinding.BindingAdapter;

public class ViewAdapter {

    /**
     * requireAll 是意思是是否需要绑定全部参数, false为否
     * View的onClick事件绑定
     * onClickCommand 绑定的命令,
     * isThrottleFirst 是否开启防止过快点击
     */
    @SuppressLint("CheckResult")
    @BindingAdapter(value = {"onClickCommand", "isThrottleFirst","countThrottle","isExpand","expandSize"}, requireAll = false)
    public static void onClickCommand(View view, final BindingCommand<Void> clickCommand, final boolean isThrottleFirst,int countThrottle,boolean isExpand,float expandSize) {
        if (isExpand){
            if (StringUtils.parseFloat(expandSize) == 0f){
                CorpseUtils.INSTANCE.expandTouchView(view,10f);
            }else {
                CorpseUtils.INSTANCE.expandTouchView(view,expandSize);
            }
        }
        if (isThrottleFirst) {
            // 修复：isThrottleFirst=true 时开启防快速点击节流（原逻辑写反，导致节流分支永不生效）
            RxView.clicks(view)
                    .throttleFirst(StringUtils.getThrottle(countThrottle), TimeUnit.SECONDS)
                    .subscribe(unit -> {
                        if (clickCommand != null) clickCommand.execute();
                    });
        } else {
            RxView.clicks(view)
                    .subscribe(unit -> {
                        if (clickCommand != null) clickCommand.execute();
                    });
        }
    }

    /**
     * view的onLongClick事件绑定
     */
    @SuppressLint("CheckResult")
    @BindingAdapter(value = {"onLongClickCommand"}, requireAll = false)
    public static void onLongClickCommand(View view, final BindingCommand<Void> clickCommand) {
        RxView.longClicks(view)
                .subscribe(unit -> {
                    if (clickCommand != null) clickCommand.execute();
                });
    }

    /**
     * 回调控件本身
     *
     * @param currentView
     * @param bindingCommand
     */
    @BindingAdapter(value = {"currentView"}, requireAll = false)
    public static void replyCurrentView(View currentView, BindingCommand<View> bindingCommand) {
        if (bindingCommand != null) {
            bindingCommand.execute(currentView);
        }
    }

    /**
     * view是否需要获取焦点
     */
    @BindingAdapter({"requestFocus"})
    public static void requestFocusCommand(View view, final Boolean needRequestFocus) {
        if (needRequestFocus) {
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        } else {
            view.clearFocus();
        }
    }

    /**
     * view的焦点发生变化的事件绑定
     */
    @BindingAdapter({"onFocusChangeCommand"})
    public static void onFocusChangeCommand(View view, final BindingCommand<Boolean> onFocusChangeCommand) {
        RxView.focusChanges(view).subscribe(hasFocus -> {
            if (onFocusChangeCommand != null) onFocusChangeCommand.execute(hasFocus);
        });
    }

    /**
     * view的显示隐藏
     */
    @BindingAdapter(value = {"isVisible"}, requireAll = false)
    public static void isVisible(View view, final Boolean visibility) {
        try {
            RxView.visibility(view).accept(visibility);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置view的最大高度
     */
    @BindingAdapter(value = {"maxHeight"}, requireAll = false)
    public static void maxHeight(View view, final int maxHeight) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new OnViewGlobalLayoutListener(view,DpiUtils.dip2px(view.getContext(),maxHeight)));
    }

    @BindingAdapter({"onTouchCommand"})
    public static void onTouchCommand(View view, final BindingCommand<MotionEvent> onTouchCommand) {
        RxView.touches(view).subscribe(motionEvent -> {
            if (onTouchCommand != null) onTouchCommand.execute(motionEvent);
        });
    }

    /**
     * 日期格式化
     * @param view                需要格式化的视图
     * @param timeFormat          文本
     * @param formatType          格式化的类型
     */
    @SuppressLint("SimpleDateFormat")
    @BindingAdapter(value = {"time_format","format_type"}, requireAll = false)
    public static void timeFormat(TextView view, final String timeFormat, final int formatType) {
        view.setText(TimeUtils.f_long_2_str(StringUtils.parseLong(timeFormat),CalendarUtil.getFormat(formatType)));
    }

    /**
     * 保留两位小数
     * @param view   显示浮点数的视图
     * @param text   必须为数字类型
     */
    @BindingAdapter(value = {"decimal_tow"}, requireAll = false)
    public static void decimalTowPoint(TextView view, String text) {
        view.setText(ViewBindingUtils.formatDecimal(text));
    }

    @BindingAdapter("android:layout_marginTop")
    public static void setTopMargin(View view, int topMargin) {
        ViewBindingUtils.setMargin(view, topMargin, ViewBindingUtils.DIRECTION_TOP);
    }

    @BindingAdapter("android:layout_marginBottom")
    public static void setBottomMargin(View view, int bottomMargin) {
        ViewBindingUtils.setMargin(view, bottomMargin, ViewBindingUtils.DIRECTION_BOTTOM);
    }

    @BindingAdapter("android:layout_marginLeft")
    public static void setLeftMargin(View view, int leftMargin) {
        ViewBindingUtils.setMargin(view, leftMargin, ViewBindingUtils.DIRECTION_LEFT);
    }

    @BindingAdapter("android:layout_marginStart")
    public static void setStartMargin(View view, int startMargin) {
        ViewBindingUtils.setMargin(view, startMargin, ViewBindingUtils.DIRECTION_START);
    }

    @BindingAdapter("android:layout_marginRight")
    public static void setRightMargin(View view, int rightMargin) {
        ViewBindingUtils.setMargin(view, rightMargin, ViewBindingUtils.DIRECTION_RIGHT);
    }

    @BindingAdapter("android:layout_marginEnd")
    public static void setEndMargin(View view, int endMargin) {
        ViewBindingUtils.setMargin(view, endMargin, ViewBindingUtils.DIRECTION_END);
    }

    @BindingAdapter("android:layout_margin")
    public static void setMargin(View view, int margin) {
        ViewBindingUtils.setMargin(view, margin, ViewBindingUtils.DIRECTION_ALL);
    }

    @BindingAdapter("android:paddingLeft")
    public static void setPaddingLeft(View view, int paddingLeft) {
        ViewBindingUtils.setPadding(view, paddingLeft, ViewBindingUtils.DIRECTION_LEFT);
    }

    @BindingAdapter("android:paddingStart")
    public static void setPaddingStart(View view, int paddingStart) {
        ViewBindingUtils.setPadding(view, paddingStart, ViewBindingUtils.DIRECTION_START);
    }

    @BindingAdapter("android:paddingRight")
    public static void setPaddingRight(View view, int paddingRight) {
        ViewBindingUtils.setPadding(view, paddingRight, ViewBindingUtils.DIRECTION_RIGHT);
    }

    @BindingAdapter("android:paddingEnd")
    public static void setPaddingEnd(View view, int paddingEnd) {
        ViewBindingUtils.setPadding(view, paddingEnd, ViewBindingUtils.DIRECTION_END);
    }

    @BindingAdapter("android:paddingTop")
    public static void setPaddingTop(View view, int paddingTop) {
        ViewBindingUtils.setPadding(view, paddingTop, ViewBindingUtils.DIRECTION_TOP);
    }

    @BindingAdapter("android:paddingBottom")
    public static void setPaddingBottom(View view, int paddingBottom) {
        ViewBindingUtils.setPadding(view, paddingBottom, ViewBindingUtils.DIRECTION_BOTTOM);
    }

    @BindingAdapter("android:padding")
    public static void setPadding(View view, int padding) {
        ViewBindingUtils.setPadding(view, padding, ViewBindingUtils.DIRECTION_ALL);
    }

    @BindingAdapter("android:gravity")
    public static void setGravity(TextView textView,int gravity){
        textView.setGravity(gravity);
    }

    @BindingAdapter("android:layout_gravity")
    public static void setLayoutGravity(View view,int gravity){
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.gravity = gravity;
        view.setLayoutParams(params);
    }

    @BindingAdapter("android:textSize")
    public static void setTextSize(TextView textView,int textSize){
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
    }

    @BindingAdapter("android:drawableRight")
    public static void setDrawableRight(TextView textView, Drawable drawable){
        ViewBindingUtils.setCompoundDrawable(textView, drawable, ViewBindingUtils.DIRECTION_RIGHT);
    }

    @BindingAdapter("android:drawableEnd")
    public static void setDrawableEnd(TextView textView, Drawable drawable){
        ViewBindingUtils.setCompoundDrawable(textView, drawable, ViewBindingUtils.DIRECTION_END);
    }

    @BindingAdapter("android:drawablePadding")
    public static void setDrawablePadding(TextView textView, float value){
        ViewBindingUtils.setDrawablePadding(textView, value);
    }

    @BindingAdapter("android:drawableLeft")
    public static void setDrawableLeft(TextView textView, Drawable drawable){
        ViewBindingUtils.setCompoundDrawable(textView, drawable, ViewBindingUtils.DIRECTION_LEFT);
    }

    @BindingAdapter("android:drawableStart")
    public static void setDrawableStart(TextView textView, Drawable drawable){
        ViewBindingUtils.setCompoundDrawable(textView, drawable, ViewBindingUtils.DIRECTION_START);
    }

    @BindingAdapter("android:drawableTop")
    public static void setDrawableTop(TextView textView, Drawable drawable){
        ViewBindingUtils.setCompoundDrawable(textView, drawable, ViewBindingUtils.DIRECTION_TOP);
    }

    @BindingAdapter("android:drawableBottom")
    public static void setDrawableBottom(TextView textView, Drawable drawable){
        ViewBindingUtils.setCompoundDrawable(textView, drawable, ViewBindingUtils.DIRECTION_BOTTOM);
    }
}
