package com.ved.framework.crash;

import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 「启动自定义错误页」策略（对应 {@code BACKGROUND_MODE_SHOW_CUSTOM}，以及所有前台崩溃）。
 * <p>
 * 从 {@code uncaughtException} 原 SHOW_CUSTOM 分支原样迁移，行为保持一致：
 * 序列化并截断堆栈（128KB，防 TransactionTooLargeException）、附带活动轨迹、
 * 按需猜测重启目标页、通知 EventListener 后启动错误页。
 */
final class LaunchErrorActivityPolicy implements CrashResponsePolicy {

    static final LaunchErrorActivityPolicy INSTANCE = new LaunchErrorActivityPolicy();

    private LaunchErrorActivityPolicy() {
    }

    @Override
    public boolean handle(CrashContext crash) {
        final Intent intent = new Intent(crash.application, crash.errorActivityClass);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        crash.throwable.printStackTrace(pw);
        String stackTraceString = sw.toString();

        //Reduce data to 128KB so we don't get a TransactionTooLargeException when sending the intent.
        //The limit is 1MB on Android but some devices seem to have it lower.
        //See: http://developer.android.com/reference/android/os/TransactionTooLargeException.html
        //And: http://stackoverflow.com/questions/11451393/what-to-do-on-transactiontoolargeexception#comment46697371_12809171
        if (stackTraceString.length() > CustomActivityOnCrash.MAX_STACK_TRACE_SIZE) {
            String disclaimer = " [stack trace too large]";
            stackTraceString = stackTraceString.substring(0, CustomActivityOnCrash.MAX_STACK_TRACE_SIZE - disclaimer.length()) + disclaimer;
        }
        intent.putExtra(CustomActivityOnCrash.EXTRA_STACK_TRACE, stackTraceString);

        if (crash.config.isTrackActivities()) {
            String activityLogString = "";
            while (!crash.activityLog.isEmpty()) {
                activityLogString += crash.activityLog.poll();
            }
            intent.putExtra(CustomActivityOnCrash.EXTRA_ACTIVITY_LOG, activityLogString);
        }

        if (crash.config.isShowRestartButton() && crash.config.getRestartActivityClass() == null) {
            //We can set the restartActivityClass because the app will terminate right now,
            //and when relaunched, will be null again by default.
            crash.config.setRestartActivityClass(CustomActivityOnCrash.guessRestartActivityClass(crash.application));
        }

        intent.putExtra(CustomActivityOnCrash.EXTRA_CONFIG, crash.config);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (crash.config.getEventListener() != null) {
            crash.config.getEventListener().onLaunchErrorActivity();
        }
        crash.application.startActivity(intent);
        return true;
    }
}
