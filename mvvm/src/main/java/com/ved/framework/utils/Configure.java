package com.ved.framework.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 全局网络配置（静态门面 + 不可变配置对象）。
 * <p>原先 4 个分散的可变静态字段被框架核心路径直接消费，多线程初始化时存在
 * 「url 已设、code 未设」的半初始化窗口；现收敛为不可变 {@link NetConfig}，
 * set 类方法构建新实例后经 volatile 引用整体原子替换，读侧永远看到自洽配置。
 * <p>对外静态 API 签名与行为完全不变。
 */
public class Configure {

    /** 不可变网络配置（volatile：写少读多的配置发布，读侧立即可见且原子） */
    private static volatile NetConfig netConfig = NetConfig.defaults();

    private Configure() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void setUrl(int code, String... url) {
        List<String> urlList = new ArrayList<>();
        if (url.length > 0) {
            urlList.addAll(Arrays.asList(url));
        }
        // 只替换地址与成功码，响应键名保持既有配置（与原独立字段语义一致）
        netConfig = netConfig.withUrl(code, urlList);
    }

    public static List<String> getUrl() {
        return netConfig.requireUrls();
    }

    public static int getCode() {
        return netConfig.getSuccessCode();
    }

    /**
     * 配置响应包装结构的字段名（默认 code / msg / data）。
     * 后台使用不同键名时（如 status / message / result），在项目初始化时调用一次即可。
     *
     * @param codeKey 业务码键名，传 null/空串表示保持默认
     * @param msgKey  消息键名，传 null/空串表示保持默认
     * @param dataKey 数据键名，传 null/空串表示保持默认
     */
    public static void setResponseKeys(String codeKey, String msgKey, String dataKey) {
        NetConfig current = netConfig;
        NetConfig updated = current;
        if (StringUtils.isNotEmpty(codeKey)) {
            updated = updated.withCodeKey(codeKey);
        }
        if (StringUtils.isNotEmpty(msgKey)) {
            updated = updated.withMsgKey(msgKey);
        }
        if (StringUtils.isNotEmpty(dataKey)) {
            updated = updated.withDataKey(dataKey);
        }
        netConfig = updated;
    }

    public static String getCodeKey() {
        return netConfig.getCodeKey();
    }

    public static String getMsgKey() {
        return netConfig.getMsgKey();
    }

    public static String getDataKey() {
        return netConfig.getDataKey();
    }

    /**
     * 不可变配置值对象：全部字段 final，任何修改都通过 with* 返回新实例（写时复制）。
     * 包私有静态嵌套类，仅 {@link Configure} 门面访问。
     */
    private static final class NetConfig {
        private final int successCode;
        private final List<String> urls;
        private final String codeKey;
        private final String msgKey;
        private final String dataKey;

        private NetConfig(int successCode, List<String> urls, String codeKey, String msgKey, String dataKey) {
            this.successCode = successCode;
            this.urls = urls;
            this.codeKey = codeKey;
            this.msgKey = msgKey;
            this.dataKey = dataKey;
        }

        /** 默认配置：无地址、成功码 0、键名 code / msg / data */
        private static NetConfig defaults() {
            return new NetConfig(0, new ArrayList<>(), "code", "msg", "data");
        }

        private NetConfig withUrl(int code, List<String> urls) {
            return new NetConfig(code, urls, this.codeKey, this.msgKey, this.dataKey);
        }

        private NetConfig withCodeKey(String codeKey) {
            return new NetConfig(this.successCode, this.urls, codeKey, this.msgKey, this.dataKey);
        }

        private NetConfig withMsgKey(String msgKey) {
            return new NetConfig(this.successCode, this.urls, this.codeKey, msgKey, this.dataKey);
        }

        private NetConfig withDataKey(String dataKey) {
            return new NetConfig(this.successCode, this.urls, this.codeKey, this.msgKey, dataKey);
        }

        private int getSuccessCode() {
            return successCode;
        }

        private String getCodeKey() {
            return codeKey;
        }

        private String getMsgKey() {
            return msgKey;
        }

        private String getDataKey() {
            return dataKey;
        }

        /**
         * 地址未配置时的守卫：抛出与原实现相同信息的异常。
         */
        private List<String> requireUrls() {
            if (urls != null && !urls.isEmpty()) {
                return urls;
            }
            throw new NullPointerException("should be set in net url");
        }
    }
}
