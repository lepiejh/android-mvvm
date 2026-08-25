package com.ved.framework.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;
import java.util.Objects;

import es.dmoral.toasty.Toasty;

/**
 * 吐司工具类（基于 Toasty 库）
 * 如果你想全局配置 Toasty 样式，可以在 Application 中配置：
 * public class MyApplication extends Application {
 *     @Override
 *     public void onCreate() {
 *         super.onCreate();
 *
 *         // 配置 Toasty 全局样式
 *         Toasty.Config.getInstance()
 *                 .setTextColor(0xFFFFFFFF)           // 文字颜色
 *                 .setToastBackground(0xDD000000)     // 背景颜色
 *                 .setTextSize(16)                    // 文字大小
 *                 .apply();
 *     }
 * }
 */
public final class ToastUtils {

    private static final int DEFAULT_COLOR = 0x12000000;
    private static Toast sToast;
    private static int gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    private static int xOffset = 0;
    private static int yOffset = (int) (64 * Utils.getContext().getResources().getDisplayMetrics().density + 0.5);
    private static int backgroundColor = DEFAULT_COLOR;
    private static int bgResource = -1;
    private static int messageColor = DEFAULT_COLOR;
    private static WeakReference<View> sViewWeakReference;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    private ToastUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    // ==================== 以下保持原有方法不变 ====================

    /**
     * 设置 Toast 的重力属性（仅适用于 API 30 及以下版本）
     *
     * @deprecated 从 Android 12 开始，文本 Toast 不再支持重力设置
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public static void setGravity(int gravity, int xOffset, int yOffset) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            KLog.w("ToastUtils", "setGravity() is ignored on API " + Build.VERSION.SDK_INT);
            return;
        }
        ToastUtils.gravity = gravity;
        ToastUtils.xOffset = xOffset;
        ToastUtils.yOffset = yOffset;
    }

    /**
     * 设置吐司view（已废弃，使用 Toasty 后不再支持自定义 View）
     *
     * @param layoutId 视图
     * @deprecated 请使用 Toasty 的样式配置替代
     */
    @Deprecated
    public static void setView(@LayoutRes int layoutId) {
        KLog.w("ToastUtils", "setView() is deprecated when using Toasty");
    }

    /**
     * 设置吐司view（已废弃，使用 Toasty 后不再支持自定义 View）
     *
     * @param view 视图
     * @deprecated 请使用 Toasty 的样式配置替代
     */
    @Deprecated
    public static void setView(@Nullable View view) {
        KLog.w("ToastUtils", "setView() is deprecated when using Toasty");
    }

    /**
     * 获取吐司view（已废弃）
     *
     * @return view
     * @deprecated 使用 Toasty 后不再支持
     */
    @Deprecated
    public static View getView() {
        return null;
    }

    /**
     * 设置背景颜色（Toasty 不支持自定义背景颜色，此方法保留兼容）
     */
    @Deprecated
    public static void setBackgroundColor(@ColorInt int backgroundColor) {
        KLog.w("ToastUtils", "setBackgroundColor() is not supported by Toasty");
        ToastUtils.backgroundColor = backgroundColor;
    }

    /**
     * 设置背景资源（Toasty 不支持自定义背景资源，此方法保留兼容）
     */
    @Deprecated
    public static void setBgResource(@DrawableRes int bgResource) {
        KLog.w("ToastUtils", "setBgResource() is not supported by Toasty");
        ToastUtils.bgResource = bgResource;
    }

    /**
     * 设置消息颜色（Toasty 不支持自定义文字颜色，此方法保留兼容）
     */
    @Deprecated
    public static void setMessageColor(@ColorInt int messageColor) {
        KLog.w("ToastUtils", "setMessageColor() is not supported by Toasty");
        ToastUtils.messageColor = messageColor;
    }

    /**
     * 设置 Toast 文字颜色（Toasty 不支持，此方法保留兼容）
     */
    @Deprecated
    public static void setTextColor(@ColorInt int color) {
        KLog.w("ToastUtils", "setTextColor() is not supported by Toasty");
    }

    /**
     * 设置 Toast 背景颜色（Toasty 不支持，此方法保留兼容）
     */
    @Deprecated
    public static void setBgColor(@ColorInt int color) {
        KLog.w("ToastUtils", "setBgColor() is not supported by Toasty");
    }

    // ==================== 安全显示方法（主线程） ====================

    public static void showShortSafe(final CharSequence text) {
        sHandler.post(() -> show(text, Toast.LENGTH_SHORT));
    }

    public static void showShortSafe(final @StringRes int resId) {
        sHandler.post(() -> show(resId, Toast.LENGTH_SHORT));
    }

    public static void showShortSafe(final @StringRes int resId, final Object... args) {
        sHandler.post(() -> {
            try {
                show(resId, Toast.LENGTH_SHORT, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void showShortSafe(final String format, final Object... args) {
        sHandler.post(() -> {
            try {
                show(format, Toast.LENGTH_SHORT, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void showLongSafe(final CharSequence text) {
        sHandler.post(() -> show(text, Toast.LENGTH_LONG));
    }

    public static void showLongSafe(final @StringRes int resId) {
        sHandler.post(() -> show(resId, Toast.LENGTH_LONG));
    }

    public static void showLongSafe(final @StringRes int resId, final Object... args) {
        sHandler.post(() -> {
            try {
                show(resId, Toast.LENGTH_LONG, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void showLongSafe(final String format, final Object... args) {
        sHandler.post(() -> {
            try {
                show(format, Toast.LENGTH_LONG, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ==================== 普通显示方法 ====================

    public static void showShort(CharSequence text) {
        try {
            show(text, Toast.LENGTH_SHORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showShort(@StringRes int resId) {
        try {
            show(resId, Toast.LENGTH_SHORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showShort(@StringRes int resId, Object... args) {
        try {
            show(resId, Toast.LENGTH_SHORT, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showShort(String format, Object... args) {
        try {
            show(format, Toast.LENGTH_SHORT, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLong(CharSequence text) {
        try {
            show(text, Toast.LENGTH_LONG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLong(@StringRes int resId) {
        try {
            show(resId, Toast.LENGTH_LONG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLong(@StringRes int resId, Object... args) {
        try {
            show(resId, Toast.LENGTH_LONG, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLong(String format, Object... args) {
        if (args.length == 0) {
            if (StringUtils.isNotEmpty(format)) {
                showLong(format);
            }
        } else {
            try {
                show(format, Toast.LENGTH_LONG, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ==================== 自定义 Toast（已废弃） ====================

    @Deprecated
    public static void showCustomShortSafe() {
        showShortSafe("");
    }

    @Deprecated
    public static void showCustomLongSafe() {
        showLongSafe("");
    }

    @Deprecated
    public static void showCustomShort() {
        showShort("");
    }

    @Deprecated
    public static void showCustomLong() {
        showLong("");
    }

    // ==================== 核心显示方法（使用 Toasty） ====================

    private static void show(@StringRes int resId, int duration) {
        try {
            show(Utils.getContext().getResources().getText(resId).toString(), duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void show(@StringRes int resId, int duration, Object... args) {
        try {
            show(String.format(Utils.getContext().getResources().getString(resId), args), duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void show(String format, int duration, Object... args) {
        try {
            show(String.format(format, args), duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 核心显示方法 - 使用 Toasty
     */
    private static void show(CharSequence text, int duration) {
        // 过滤无效文本
        if (StringUtils.isSpace(StringUtils.parseStr(text))) return;

        String error = SPUtils.getInstance().getString("error", "");
        if (Objects.equals(error, StringUtils.parseStr(text))) return;
        if (Objects.equals("Forbidden", StringUtils.parseStr(text))) return;
        if (Objects.equals("未知错误", StringUtils.parseStr(text))) return;
        if (Objects.equals("null", StringUtils.parseStr(text))) return;

        try {
            // 取消旧的 Toast
            cancel();

            Context context = Utils.getContext();

            // 创建普通 Toast，不显示图标
            if (duration == Toast.LENGTH_LONG) {
                Toasty.normal(context, text, Toast.LENGTH_LONG).show();
            } else {
                Toasty.normal(context, text, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 降级方案：使用系统 Toast
            fallbackShowSystemToast(text, duration);
        }
    }

    /**
     * 降级方案：当 Toasty 失败时使用系统 Toast
     */
    @SuppressLint("ShowToast")
    private static void fallbackShowSystemToast(CharSequence text, int duration) {
        try {
            if (sToast == null) {
                sToast = Toast.makeText(Utils.getContext(), text, duration);
            } else {
                sToast.setText(text);
                sToast.setDuration(duration);
            }
            sToast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消吐司显示
     */
    public static void cancel() {
        try {
            if (sToast != null) {
                sToast.cancel();
                sToast = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 额外功能方法 ====================

    /**
     * 显示成功 Toast
     */
    public static void showSuccess(CharSequence text) {
        try {
            if (StringUtils.isSpace(StringUtils.parseStr(text))) return;
            Toasty.success(Utils.getContext(), text, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示错误 Toast
     */
    public static void showError(CharSequence text) {
        try {
            if (StringUtils.isSpace(StringUtils.parseStr(text))) return;
            Toasty.error(Utils.getContext(), text, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示警告 Toast
     */
    public static void showWarning(CharSequence text) {
        try {
            if (StringUtils.isSpace(StringUtils.parseStr(text))) return;
            Toasty.warning(Utils.getContext(), text, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示信息 Toast
     */
    public static void showInfo(CharSequence text) {
        try {
            if (StringUtils.isSpace(StringUtils.parseStr(text))) return;
            Toasty.info(Utils.getContext(), text, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 兼容内部类 ====================

    public static final class UtilsMaxWidthRelativeLayout extends RelativeLayout {

        private static final int SPACING = ScreenUtils.dp2px(80);

        public UtilsMaxWidthRelativeLayout(Context context) {
            super(context);
        }

        public UtilsMaxWidthRelativeLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public UtilsMaxWidthRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthMaxSpec = MeasureSpec.makeMeasureSpec(ScreenUtils.getAppScreenWidth() - SPACING, MeasureSpec.AT_MOST);
            super.onMeasure(widthMaxSpec, heightMeasureSpec);
        }
    }
}