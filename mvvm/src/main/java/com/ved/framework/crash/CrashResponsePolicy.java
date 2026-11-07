package com.ved.framework.crash;

/**
 * 崩溃响应策略（策略模式）：
 * 把原 {@code uncaughtException} 中按 BACKGROUND_MODE / 崩溃循环 / 冲突栈
 * 展开的 70 行嵌套 if-else，拆成三个可独立演化的处置策略——
 * <ul>
 *     <li>{@link LaunchErrorActivityPolicy}：启动自定义错误页（SHOW_CUSTOM 或前台崩溃）</li>
 *     <li>{@link DelegatePreviousHandlerPolicy}：交还前序 handler（CRASH 模式、崩溃循环、冲突栈共用）</li>
 *     <li>{@link SilentCrashPolicy}：静默崩溃（SILENT 模式）</li>
 * </ul>
 * 新增崩溃处置方式（如上报到崩溃平台后再重启）时，实现本接口并在
 * {@code CustomActivityOnCrash} 的分派点注册即可，无需再动分发流程。
 */
interface CrashResponsePolicy {

    /**
     * 执行崩溃响应动作。
     *
     * @return true  表示策略已接管完毕，调用方应继续执行「finish 最后的 Activity + killCurrentProcess」收尾；
     *         false 表示崩溃已交还给前序 UncaughtExceptionHandler，调用方应立即 return。
     */
    boolean handle(CrashContext crash);
}
