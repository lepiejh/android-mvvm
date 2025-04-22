package com.ved.framework.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Configure {
    private static List<String> url;
    private static String packageName;
    private static int code;

    private Configure() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void setUrl(String... url){
        List<String> urlList = new ArrayList<>();
        if (url.length > 0){
            urlList.addAll(Arrays.asList(url));
        }
        Configure.url = urlList;
    }

    public static void setCode(int code){
        Configure.code = code;
    }

    public static void setPackageName(String packageName){
        Configure.packageName = packageName;
    }

    public static List<String> getUrl() {
        if (url != null && url.size() > 0) {
            return url;
        }
        throw new NullPointerException("should be set in net url");
    }

    public static String getPackageName() {
        if (!TextUtils.isEmpty(packageName)) {
            return packageName;
        }
        throw new NullPointerException("should be set in packageName");
    }

    public static int getCode() {
        return code;
    }
}
