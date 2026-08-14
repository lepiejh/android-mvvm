package com.ved.framework.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Configure {
    private static List<String> url;
    private static int code;
    // 响应包装结构字段名配置（不同后台键名可能不同），默认 code/msg/data
    private static String codeKey = "code";
    private static String msgKey = "msg";
    private static String dataKey = "data";

    private Configure() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void setUrl(int code,String... url){
        List<String> urlList = new ArrayList<>();
        if (url.length > 0){
            urlList.addAll(Arrays.asList(url));
        }
        Configure.url = urlList;
        Configure.code = code;
    }

    public static List<String> getUrl() {
        if (url != null && url.size() > 0) {
            return url;
        }
        throw new NullPointerException("should be set in net url");
    }

    public static int getCode() {
        return code;
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
        if (StringUtils.isNotEmpty(codeKey)) {
            Configure.codeKey = codeKey;
        }
        if (StringUtils.isNotEmpty(msgKey)) {
            Configure.msgKey = msgKey;
        }
        if (StringUtils.isNotEmpty(dataKey)) {
            Configure.dataKey = dataKey;
        }
    }

    public static String getCodeKey() {
        return codeKey;
    }

    public static String getMsgKey() {
        return msgKey;
    }

    public static String getDataKey() {
        return dataKey;
    }
}
