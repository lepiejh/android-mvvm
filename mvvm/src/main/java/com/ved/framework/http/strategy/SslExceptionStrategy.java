package com.ved.framework.http.strategy;

import com.ved.framework.http.ErrorCode;

import javax.net.ssl.SSLException;

/**
 * SSL 证书异常策略
 */
public class SslExceptionStrategy extends AbstractExceptionStrategy {

    @Override
    protected boolean matchType(Throwable e) {
        return e instanceof SSLException;
    }

    @Override
    protected int code() {
        return ErrorCode.SSL_ERROR;
    }

    @Override
    protected String message() {
        return "证书验证失败";
    }
}
