package com.ved.framework.http.strategy;

import com.ved.framework.http.ResponseThrowable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 异常处理策略注册中心
 *
 * 采用责任链方式依次匹配策略，命中第一个可处理的策略即返回。
 * 策略顺序很重要：通用策略（如 {@link UnknownExceptionStrategy}）必须放在最后。
 */
public final class ExceptionHandlerRegistry {

    private static final List<IExceptionStrategy> STRATEGIES = new ArrayList<>();

    static {
        register(new HttpExceptionStrategy());
        register(new ParseExceptionStrategy());
        register(new ConnectExceptionStrategy());
        register(new SslExceptionStrategy());
        register(new TimeoutExceptionStrategy());
        // 兜底策略必须最后注册
        register(new UnknownExceptionStrategy());
    }

    private ExceptionHandlerRegistry() {
    }

    /**
     * 注册自定义异常处理策略（扩展点，开闭原则）
     */
    public static void register(IExceptionStrategy strategy) {
        if (strategy != null) {
            STRATEGIES.add(strategy);
        }
    }

    /**
     * 注册自定义策略集合
     */
    public static void registerAll(IExceptionStrategy... strategies) {
        if (strategies != null && strategies.length > 0) {
            STRATEGIES.addAll(Arrays.asList(strategies));
        }
    }

    /**
     * 依次匹配策略并处理异常，默认返回未知错误策略
     */
    public static ResponseThrowable handle(Throwable e) {
        for (IExceptionStrategy strategy : STRATEGIES) {
            if (strategy.matches(e)) {
                return strategy.handle(e);
            }
        }
        return new UnknownExceptionStrategy().handle(e);
    }
}
