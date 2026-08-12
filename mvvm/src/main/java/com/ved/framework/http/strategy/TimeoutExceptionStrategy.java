package com.ved.framework.http.strategy;

import com.ved.framework.http.ErrorCode;
import com.ved.framework.http.ResponseThrowable;

import org.apache.http.conn.ConnectTimeoutException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 超时异常策略（连接超时 / 读取超时 / 主机未知）
 */
public class TimeoutExceptionStrategy extends AbstractExceptionStrategy {

    @Override
    protected boolean matchType(Throwable e) {
        return e instanceof ConnectTimeoutException
                || e instanceof SocketTimeoutException
                || e instanceof UnknownHostException;
    }

    @Override
    protected int code() {
        return ErrorCode.TIMEOUT_ERROR;
    }

    @Override
    protected String message() {
        return "连接超时";
    }

    @Override
    public ResponseThrowable handle(Throwable e) {
        ResponseThrowable ex = new ResponseThrowable(e, code());
        if (e instanceof UnknownHostException) {
            ex.message = "主机地址未知";
        } else {
            ex.message = message();
        }
        return ex;
    }
}
