package com.ved.framework.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Objects;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

/**
 * 吐司工具类
 */
public final class ToastUtils {

    private static final int DEFAULT_COLOR = 0x12000000;
    private static Toast sToast;
    private static int gravity         = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    private static int xOffset         = 0;
    private static int yOffset         = (int) (64 * Utils.getContext().getResources().getDisplayMetrics().density + 0.5);
    private static int backgroundColor = DEFAULT_COLOR;
    private static int bgResource      = -1;
    private static int messageColor    = DEFAULT_COLOR;
    private static WeakReference<View> sViewWeakReference;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    private ToastUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 设置 Toast 的重力属性（仅适用于 API 30 及以下版本）
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
     * 设置吐司view
     *
     * @param layoutId 视图
     */
    public static void setView(@LayoutRes int layoutId) {
        LayoutInflater inflate = (LayoutInflater) Utils.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sViewWeakReference = new WeakReference<>(inflate.inflate(layoutId, null));
    }

    /**
     * 设置吐司view
     *
     * @param view 视图
     */
    public static void setView(@Nullable View view) {
        sViewWeakReference = view == null ? null : new WeakReference<>(view);
    }

    /**
     * 获取吐司view
     *
     * @return view
     */
    public static View getView() {
        if (sViewWeakReference != null) {
            final View view = sViewWeakReference.get();
            if (view != null) {
                return view;
            }
        }
        if (sToast != null) return sToast.getView();
        return null;
    }

    /**
     * 设置背景颜色
     *
     * @param backgroundColor 背景色
     */
    public static void setBackgroundColor(@ColorInt int backgroundColor) {
        ToastUtils.backgroundColor = backgroundColor;
    }

    /**
     * 设置背景资源
     *
     * @param bgResource 背景资源
     */
    public static void setBgResource(@DrawableRes int bgResource) {
        ToastUtils.bgResource = bgResource;
    }

    /**
     * 设置消息颜色
     *
     * @param messageColor 颜色
     */
    public static void setMessageColor(@ColorInt int messageColor) {
        ToastUtils.messageColor = messageColor;
    }

    /**
     * 安全地显示短时吐司
     *
     * @param text 文本
     */
    public static void showShortSafe(final CharSequence text) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                show(text, Toast.LENGTH_SHORT);
            }
        });
    }

    /**
     * 安全地显示短时吐司
     *
     * @param resId 资源Id
     */
    public static void showShortSafe(final @StringRes int resId) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                show(resId, Toast.LENGTH_SHORT);
            }
        });
    }

    /**
     * 安全地显示短时吐司
     *
     * @param resId 资源Id
     * @param args  参数
     */
    public static void showShortSafe(final @StringRes int resId, final Object... args) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    show(resId, Toast.LENGTH_SHORT, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 安全地显示短时吐司
     *
     * @param format 格式
     * @param args   参数
     */
    public static void showShortSafe(final String format, final Object... args) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    show(format, Toast.LENGTH_SHORT, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 安全地显示长时吐司
     *
     * @param text 文本
     */
    public static void showLongSafe(final CharSequence text) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                show(text, Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * 安全地显示长时吐司
     *
     * @param resId 资源Id
     */
    public static void showLongSafe(final @StringRes int resId) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                show(resId, Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * 安全地显示长时吐司
     *
     * @param resId 资源Id
     * @param args  参数
     */
    public static void showLongSafe(final @StringRes int resId, final Object... args) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    show(resId, Toast.LENGTH_LONG, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 安全地显示长时吐司
     *
     * @param format 格式
     * @param args   参数
     */
    public static void showLongSafe(final String format, final Object... args) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    show(format, Toast.LENGTH_LONG, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 显示短时吐司
     *
     * @param text 文本
     */
    public static void showShort(CharSequence text) {
        try {
            show(text, Toast.LENGTH_SHORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示短时吐司
     *
     * @param resId 资源Id
     */
    public static void showShort(@StringRes int resId) {
        try {
            show(resId, Toast.LENGTH_SHORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示短时吐司
     *
     * @param resId 资源Id
     * @param args  参数
     */
    public static void showShort(@StringRes int resId, Object... args) {
        try {
            show(resId, Toast.LENGTH_SHORT, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示短时吐司
     *
     * @param format 格式
     * @param args   参数
     */
    public static void showShort(String format, Object... args) {
        try {
            show(format, Toast.LENGTH_SHORT, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示长时吐司
     *
     * @param text 文本
     */
    public static void showLong(CharSequence text) {
        try {
            show(text, Toast.LENGTH_LONG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示长时吐司
     *
     * @param resId 资源Id
     */
    public static void showLong(@StringRes int resId) {
        try {
            show(resId, Toast.LENGTH_LONG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示长时吐司
     *
     * @param resId 资源Id
     * @param args  参数
     */
    public static void showLong(@StringRes int resId, Object... args) {
        try {
            show(resId, Toast.LENGTH_LONG, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示长时吐司
     *
     * @param format 格式
     * @param args   参数
     */
    public static void showLong(String format, Object... args) {
        if (args.length == 0){
            if (StringUtils.isNotEmpty(format)){
                showLong(format);
            }
        }else {
            try {
                show(format, Toast.LENGTH_LONG, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 安全地显示短时自定义吐司
     */
    public static void showCustomShortSafe() {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    show("", Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 安全地显示长时自定义吐司
     */
    public static void showCustomLongSafe() {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    show("", Toast.LENGTH_LONG);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 显示短时自定义吐司
     */
    public static void showCustomShort() {
        try {
            show("", Toast.LENGTH_SHORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示长时自定义吐司
     */
    public static void showCustomLong() {
        try {
            show("", Toast.LENGTH_LONG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示吐司
     *
     * @param resId    资源Id
     * @param duration 显示时长
     */
    private static void show(@StringRes int resId, int duration) {
        try {
            show(Utils.getContext().getResources().getText(resId).toString(), duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示吐司
     *
     * @param resId    资源Id
     * @param duration 显示时长
     * @param args     参数
     */
    private static void show(@StringRes int resId, int duration, Object... args) {
        try {
            show(String.format(Utils.getContext().getResources().getString(resId), args), duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示吐司
     *
     * @param format   格式
     * @param duration 显示时长
     * @param args     参数
     */
    private static void show(String format, int duration, Object... args) {
        try {
            show(String.format(format, args), duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示一个toast提示
     *
     * @param context  context 上下文对象
     * @param text     toast字符串
     * @param duration toast显示时间
     */
    @SuppressLint("ShowToast")
    public static void showToast(Context context, CharSequence text, int duration) {
        //Android9.0系统已处理，没有该问题，Android10.0又改回9.0以前的实现
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P || sToast == null) {
            sToast = Toast.makeText(context, text, duration);
        } else {
            sToast.setText(text);
            sToast.setDuration(duration);
        }

    }

    /**
     * 显示吐司
     *
     * @param text     文本
     * @param duration 显示时长
     */
    @SuppressLint("ShowToast")
    private static void show(CharSequence text, int duration) {
        if (StringUtils.isSpace(StringUtils.parseStr(text)))return;
        String error = SPUtils.getInstance().getString("error","");
        if (Objects.equals(error,StringUtils.parseStr(text)))return;
        if (Objects.equals("Forbidden",StringUtils.parseStr(text)))return;
        if (Objects.equals("未知错误",StringUtils.parseStr(text)))return;
        if (Objects.equals("null",StringUtils.parseStr(text)))return;
        CorpseUtils.INSTANCE.handlerThread(() -> {
            showToast(text, duration);
            return null;
        });
    }

    private static void showToast(CharSequence text, int duration) {
        try {
            cancel();
            boolean isCustom = false;
            if (sViewWeakReference != null) {
                final View view = sViewWeakReference.get();
                if (view != null) {
                    sToast = new Toast(Utils.getContext());
                    sToast.setView(view);
                    sToast.setDuration(duration);
                    isCustom = true;
                }
            }
            if (!isCustom) {
                if (messageColor != DEFAULT_COLOR) {
                    SpannableString spannableString = new SpannableString(text);
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(messageColor);
                    spannableString.setSpan(colorSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    showToast(Utils.getContext(), spannableString, duration);
                } else {
                    showToast(Utils.getContext(), text, duration);
                }
            }
            View view = sToast.getView();
            if (bgResource != -1) {
                view.setBackgroundResource(bgResource);
            } else if (backgroundColor != DEFAULT_COLOR) {
                view.setBackgroundColor(backgroundColor);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                sToast.setGravity(gravity, xOffset, yOffset);
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
        if (sToast != null) {
            sToast.cancel();
            sToast = null;
        }
    }
}

