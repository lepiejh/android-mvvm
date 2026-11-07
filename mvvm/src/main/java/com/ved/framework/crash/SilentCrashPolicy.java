package com.ved.framework.crash;

/**
 * 「静默崩溃」策略（对应 {@code BACKGROUND_MODE_SILENT}）：
 * 不启动错误页、也不交还前序 handler，直接放行给调用方执行
 * 「finish 最后的 Activity + killCurrentProcess」收尾。
 * <p>
 * 与原实现的注释一致：本策略什么都不做，进程由收尾逻辑结束。
 */
final class SilentCrashPolicy implements CrashResponsePolicy {

    static final SilentCrashPolicy INSTANCE = new SilentCrashPolicy();

    private SilentCrashPolicy() {
    }

    @Override
    public boolean handle(CrashContext crash) {
        //Else (BACKGROUND_MODE_SILENT): do nothing and let the following code kill the process
        return true;
    }
}
