package com.ved.framework.http.strategy;

import com.ved.framework.http.ErrorCode;

/**
 * 未知异常兜底策略（责任链末端）
 */
public class UnknownExceptionStrategy extends AbstractExceptionStrategy {

    @Override
    protected boolean matchType(Throwable e) {
        return true;
    }

    @Override
    protected int code() {
        return ErrorCode.UNKNOWN;
    }

    @Override
    protected String message() {
        return "未知错误";
    }
}
