package com.ved.framework.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * 应用程序信息工具类
 */
public class AppInfoUtil {

    public static int getLocalVersion(Context ctx) {
        int localVersion = 0;
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 获取本地软件版本号名称
     */
    public static String getLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 比较版本号的大小,前者大则返回一个正数,后者大返回一个负数,相等则返回0
     * 逐段按数值比较, 如 "1.0.99" < "1.0.100"
     * @param version1    本地的版本号
     * @param version2    服务器的版本号
     * @return
     */
    public static int compareVersion(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return 0;
        }
        String[] versionArray1 = version1.split("\\.");//注意此处为正则匹配，不能用"."；
        String[] versionArray2 = version2.split("\\.");
        int maxLength = Math.max(versionArray1.length, versionArray2.length);
        for (int i = 0; i < maxLength; i++) {
            // 越界的段按 0 处理, 如 "1.0" 与 "1.0.0" 相等
            int seg1 = i < versionArray1.length ? parseVersionSegment(versionArray1[i]) : 0;
            int seg2 = i < versionArray2.length ? parseVersionSegment(versionArray2[i]) : 0;
            if (seg1 != seg2) {
                return seg1 - seg2;
            }
        }
        return 0;
    }

    private static int parseVersionSegment(String segment) {
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
