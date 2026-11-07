package com.ved.framework.net;

import okhttp3.OkHttpClient;

/**
 * 网络底层能力门面（单例 + 门面模式）：
 * 向框架内其他包暴露全局共享的底层 OkHttpClient，供下载等旁路网络能力复用
 * 连接池、缓存、Cookie、SSL 等组件（享元思想）。
 * <p>
 * {@link RetrofitClient} 保持包私有，跨包获取底层 client 一律经过本类，
 * 避免把 Retrofit 的服务创建/请求编排细节扩散到包外。
 * <p>
 * 使用约定：调用方必须通过 {@code getBaseClient().newBuilder()...build()} 派生新
 * client——{@code newBuilder()} 是浅拷贝，派生过程不会影响全局实例本身；
 * 严禁把返回值直接改配后当作全局 client 使用。
 */
public final class HttpClientProvider {

    private HttpClientProvider() {
    }

    // 静态内部类单例：由 JVM 保证类加载时的线程安全，且延迟到首次使用时才初始化
    private static class SingletonHolder {
        private static final HttpClientProvider INSTANCE = new HttpClientProvider();
    }

    public static HttpClientProvider getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 获取全局共享的基础 OkHttpClient（含连接池/缓存/Cookie/SSL/超时配置）。
     *
     * @return 全局共享的基础 OkHttpClient
     */
    public OkHttpClient getBaseClient() {
        return RetrofitClient.getBaseClient();
    }
}
