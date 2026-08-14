package com.ved.framework.utils;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DecimalFormat;

import androidx.annotation.IntDef;

/**
 * 视图绑定工具类
 * 提取 ViewAdapter 中重复的 margin / padding / drawable / 格式化逻辑
 */
public final class ViewBindingUtils {

    /** 方向：左 */
    public static final int DIRECTION_LEFT = 0;
    /** 方向：上 */
    public static final int DIRECTION_TOP = 1;
    /** 方向：右 */
    public static final int DIRECTION_RIGHT = 2;
    /** 方向：下 */
    public static final int DIRECTION_BOTTOM = 3;
    /** 方向：起始（等同 LEFT） */
    public static final int DIRECTION_START = 4;
    /** 方向：结尾（等同 RIGHT） */
    public static final int DIRECTION_END = 5;
    /** 方向：四周全部 */
    public static final int DIRECTION_ALL = 6;

    @IntDef({DIRECTION_LEFT, DIRECTION_TOP, DIRECTION_RIGHT, DIRECTION_BOTTOM,
            DIRECTION_START, DIRECTION_END, DIRECTION_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
    }

    /** 享元模式：复用 DecimalFormat，避免每次格式化都 new */
    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("######0.00");
        }
    };

    private ViewBindingUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 设置 margin（dp 值，自动转 px）
     *
     * @param view      目标 View
     * @param margin    margin 值（dp）
     * @param direction 方向，取值 {@link Direction}
     */
    public static void setMargin(View view, int margin, @Direction int direction) {
        if (view == null) return;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (lp == null) return;
        int px = DpiUtils.dip2px(view.getContext(), margin);
        switch (direction) {
            case DIRECTION_LEFT:
            case DIRECTION_START:
                lp.setMargins(px, lp.topMargin, lp.rightMargin, lp.bottomMargin);
                break;
            case DIRECTION_TOP:
                lp.setMargins(lp.leftMargin, px, lp.rightMargin, lp.bottomMargin);
                break;
            case DIRECTION_RIGHT:
            case DIRECTION_END:
                lp.setMargins(lp.leftMargin, lp.topMargin, px, lp.bottomMargin);
                break;
            case DIRECTION_BOTTOM:
                lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, px);
                break;
            case DIRECTION_ALL:
                lp.setMargins(px, px, px, px);
                break;
            default:
                break;
        }
        view.setLayoutParams(lp);
    }

    /**
     * 设置 padding（dp 值，自动转 px）
     *
     * @param view     目标 View
     * @param padding  padding 值（dp）
     * @param direction 方向，取值 {@link Direction}
     */
    public static void setPadding(View view, int padding, @Direction int direction) {
        if (view == null) return;
        int px = DpiUtils.dip2px(view.getContext(), padding);
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        switch (direction) {
            case DIRECTION_LEFT:
            case DIRECTION_START:
                left = px;
                break;
            case DIRECTION_TOP:
                top = px;
                break;
            case DIRECTION_RIGHT:
            case DIRECTION_END:
                right = px;
                break;
            case DIRECTION_BOTTOM:
                bottom = px;
                break;
            case DIRECTION_ALL:
                left = top = right = bottom = px;
                break;
            default:
                break;
        }
        view.setPadding(left, top, right, bottom);
    }

    /**
     * 设置 TextView 的 compound drawable（自动设置 bounds）
     *
     * @param textView  目标 TextView
     * @param drawable  drawable，为 null 时直接忽略
     * @param direction 方向，取值 {@link Direction}
     */
    public static void setCompoundDrawable(TextView textView, Drawable drawable, @Direction int direction) {
        if (textView == null || drawable == null) return;
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        switch (direction) {
            case DIRECTION_LEFT:
            case DIRECTION_START:
                textView.setCompoundDrawables(drawable, null, null, null);
                break;
            case DIRECTION_TOP:
                textView.setCompoundDrawables(null, drawable, null, null);
                break;
            case DIRECTION_RIGHT:
            case DIRECTION_END:
                textView.setCompoundDrawables(null, null, drawable, null);
                break;
            case DIRECTION_BOTTOM:
                textView.setCompoundDrawables(null, null, null, drawable);
                break;
            default:
                break;
        }
    }

    /**
     * 设置 compound drawable 与文本之间的间距（dp 值）
     *
     * @param textView 目标 TextView
     * @param value    间距（dp）
     */
    public static void setDrawablePadding(TextView textView, float value) {
        if (textView == null) return;
        int paddingPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, textView.getResources().getDisplayMetrics());
        textView.setCompoundDrawablePadding(paddingPx);
    }

    /**
     * 保留两位小数格式化，非法输入返回 "0.00"
     *
     * @param text 数字文本
     * @return 格式化结果
     */
    public static String formatDecimal(String text) {
        if (TextUtils.isEmpty(text)) return "0.00";
        try {
            return DECIMAL_FORMAT.get().format(StringUtils.parseDouble(text));
        } catch (Exception e) {
            return "0.00";
        }
    }
}
