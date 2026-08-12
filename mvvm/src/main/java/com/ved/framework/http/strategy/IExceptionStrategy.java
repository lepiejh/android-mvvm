package com.ved.framework.http.strategy;

import com.ved.framework.http.ResponseThrowable;

/**
 * 异常处理策略接口
 *
 * 策略模式：将 ExceptionHandle 中大量的 if-else 异常判断
 * 拆分为独立的策略实现，每种异常类型对应一个策略，
 * 新增异常类型时只需新增一个策略实现类，无需修改已有代码（开闭原则）。
 */
public interface IExceptionStrategy {

    /**
     * 判断当前策略是否能够处理该异常
     *
     * @param e 原始异常
     * @return true 表示可以处理
     */
    boolean matches(Throwable e);

    /**
     * 将原始异常转换为框架约定的 {@link ResponseThrowable}
     *
     * @param e 原始异常
     * @return 转换后的响应异常
     */
    ResponseThrowable handle(Throwable e);
}
