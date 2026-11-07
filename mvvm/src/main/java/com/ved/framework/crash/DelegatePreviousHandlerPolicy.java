package com.ved.framework.crash;

/**
 * 「交还前序 handler」策略：崩溃转交给安装本组件之前的
 * {@link Thread.UncaughtExceptionHandler}（通常是系统默认 handler）。
 * <p>
 * 三种场景共用本策略，行为与原实现逐分支一致：
 * <ul>
 *     <li>{@code BACKGROUND_MODE_CRASH}：后台崩溃时走系统默认崩溃对话框；</li>
 *     <li>崩溃循环（短时间二次崩溃）：避免反复拉起错误页进入重启死循环；</li>
 *     <li>冲突栈（Application 初始化期或错误页自身崩溃）：错误页大概率也起不来。</li>
 * </ul>
 * 前序 handler 不存在（不应发生）时返回 true，让调用方继续执行
 * 「finish + killCurrentProcess」收尾——与原实现的「不 return、向下穿透」语义一致，
 * 否则进程会卡死在崩溃现场。
 */
final class DelegatePreviousHandlerPolicy implements CrashResponsePolicy {

    static final DelegatePreviousHandlerPolicy INSTANCE = new DelegatePreviousHandlerPolicy();

    private DelegatePreviousHandlerPolicy() {
    }

    @Override
    public boolean handle(CrashContext crash) {
        if (crash.previousHandler != null) {
            crash.previousHandler.uncaughtException(crash.thread, crash.throwable);
            return false;
        }
        return true;
    }
}
