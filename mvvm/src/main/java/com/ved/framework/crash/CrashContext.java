package com.ved.framework.crash;

import android.app.Activity;
import android.app.Application;

import java.util.Deque;

import androidx.annotation.Nullable;

/**
 * 崩溃上下文（参数对象）：{@link CrashResponsePolicy} 各策略执行时需要的现场数据。
 * <p>
 * 把原 {@code uncaughtException} 里散落在闭包中的状态（application / config /
 * activityLog / isInBackground 等）收拢为一个不可变对象，策略实现因此可以是
 * 无状态单例，便于注册表化分派与单元测试。
 */
final class CrashContext {

    final Application application;
    final Thread thread;
    final Throwable throwable;
    /** 崩溃发生前已安装的 UncaughtExceptionHandler，可能为 null */
    @Nullable
    final Thread.UncaughtExceptionHandler previousHandler;
    /** 当前生效的崩溃配置 */
    final CaocConfig config;
    /**
     * 解析出的错误页目标 Activity（config 未配置时已由 {@code guessErrorActivityClass} 兜底）。
     * 仅当走到「决定是否启动错误页」这一步时才保证非 null；崩溃循环等提前出口处为 null。
     */
    @Nullable
    final Class<? extends Activity> errorActivityClass;
    /** 崩溃发生时 App 是否处于后台（由生命周期回调维护） */
    final boolean isInBackground;
    /** 活动轨迹日志（config.isTrackActivities() 时用于错误页展示，策略会消费它） */
    final Deque<String> activityLog;

    CrashContext(Application application,
                 Thread thread,
                 Throwable throwable,
                 @Nullable Thread.UncaughtExceptionHandler previousHandler,
                 CaocConfig config,
                 @Nullable Class<? extends Activity> errorActivityClass,
                 boolean isInBackground,
                 Deque<String> activityLog) {
        this.application = application;
        this.thread = thread;
        this.throwable = throwable;
        this.previousHandler = previousHandler;
        this.config = config;
        this.errorActivityClass = errorActivityClass;
        this.isInBackground = isInBackground;
        this.activityLog = activityLog;
    }
}
