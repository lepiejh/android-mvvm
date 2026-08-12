package com.ved.framework.http;

/**
 * 框架约定的错误码常量
 *
 * 从 {@link ExceptionHandle.ERROR} 中提取，便于各策略实现类独立引用，
 * 同时保留 {@link ExceptionHandle.ERROR} 以保证对外 API 兼容。
 */
public interface ErrorCode {
    /**
     * 未知错误
     */
    int UNKNOWN = 1000;
    /**
     * 解析错误
     */
    int PARSE_ERROR = 1001;
    /**
     * 网络错误
     */
    int NETWORD_ERROR = 1002;
    /**
     * 协议出错
     */
    int HTTP_ERROR = 1003;

    /**
     * 证书出错
     */
    int SSL_ERROR = 1005;

    /**
     * 连接超时
     */
    int TIMEOUT_ERROR = 1006;
}
