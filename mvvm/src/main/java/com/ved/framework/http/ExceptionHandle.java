package com.ved.framework.http;

import com.ved.framework.http.strategy.ExceptionHandlerRegistry;
import com.ved.framework.utils.KLog;

/**
 * Created by ved on 2017/5/11.
 *
 * 异常处理门面
 *
 * 内部实现已重构为策略模式，各策略类见 {@link com.ved.framework.http.strategy} 包，
 * 本类仅保留对外统一的入口与错误码常量，保证框架 API 兼容。
 */
public class ExceptionHandle {

    public static ResponseThrowable handleException(Throwable e) {
        KLog.e("--NET--", "request network error : " + e);
        return ExceptionHandlerRegistry.handle(e);
    }

    /**
     * 约定异常 这个具体规则需要与服务端或者领导商讨定义
     */
    public static class ERROR {
        /**
         * 未知错误
         */
        public static final int UNKNOWN = ErrorCode.UNKNOWN;
        /**
         * 解析错误
         */
        public static final int PARSE_ERROR = ErrorCode.PARSE_ERROR;
        /**
         * 网络错误
         */
        public static final int NETWORD_ERROR = ErrorCode.NETWORD_ERROR;
        /**
         * 协议出错
         */
        public static final int HTTP_ERROR = ErrorCode.HTTP_ERROR;

        /**
         * 证书出错
         */
        public static final int SSL_ERROR = ErrorCode.SSL_ERROR;

        /**
         * 连接超时
         */
        public static final int TIMEOUT_ERROR = ErrorCode.TIMEOUT_ERROR;
    }
}
