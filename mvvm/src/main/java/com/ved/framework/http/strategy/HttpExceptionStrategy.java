package com.ved.framework.http.strategy;

import com.ved.framework.http.ErrorCode;
import com.ved.framework.http.ResponseThrowable;

import java.util.HashMap;
import java.util.Map;

import retrofit2.HttpException;

/**
 * HTTP 协议异常策略
 *
 * 将原 {@code switch (httpException.code())} 分支替换为
 * 状态码 -> 提示信息 的映射表，新增状态码只需添加映射项。
 */
public class HttpExceptionStrategy extends AbstractExceptionStrategy {

    private static final Map<Integer, String> STATUS_MESSAGES = new HashMap<>();

    static {
        STATUS_MESSAGES.put(401, "操作未授权");
        STATUS_MESSAGES.put(403, "请求被拒绝");
        STATUS_MESSAGES.put(404, "资源不存在");
        STATUS_MESSAGES.put(408, "服务器执行超时");
        STATUS_MESSAGES.put(500, "服务器内部错误");
        STATUS_MESSAGES.put(503, "服务器不可用");
    }

    @Override
    protected boolean matchType(Throwable e) {
        return e instanceof HttpException;
    }

    @Override
    protected int code() {
        return ErrorCode.HTTP_ERROR;
    }

    @Override
    protected String message() {
        return "网络错误";
    }

    @Override
    public ResponseThrowable handle(Throwable e) {
        ResponseThrowable ex = new ResponseThrowable(e, code());
        if (e instanceof HttpException) {
            int httpCode = ((HttpException) e).code();
            String msg = STATUS_MESSAGES.get(httpCode);
            ex.message = msg != null ? msg : message();
        } else {
            ex.message = message();
        }
        return ex;
    }
}
