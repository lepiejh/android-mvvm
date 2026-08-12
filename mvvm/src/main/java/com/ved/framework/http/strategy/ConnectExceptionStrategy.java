package com.ved.framework.http.strategy;

import com.ved.framework.http.ErrorCode;

import java.net.ConnectException;

/**
 * 网络连接异常策略
 */
public class ConnectExceptionStrategy extends AbstractExceptionStrategy {

    @Override
    protected boolean matchType(Throwable e) {
        return e instanceof ConnectException;
    }

    @Override
    protected int code() {
        return ErrorCode.NETWORD_ERROR;
    }

    @Override
    protected String message() {
        return "连接失败";
    }
}
