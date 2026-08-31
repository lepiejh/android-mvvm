package com.ved.framework.utils;

import android.util.Base64;

/**
 * 加密公共辅助方法，供 AesCbcUtil / ThreeDesCbcUtil 等对称加密类共享使用。
 */
public final class CryptoHelper {

    private CryptoHelper() {
    }

    /**
     * URL-safe Base64 编码（无 padding）
     */
    public static String urlSafeBase64Encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP)
                .replace('+', '-')
                .replace('/', '_')
                .replace("=", "");
    }

    /**
     * Base64 预处理：去除非法字符、还原 URL-safe 字符、补全 padding。
     *
     * @return 处理后的标准 Base64 字符串，输入无效时返回 null
     */
    public static String preprocessBase64(String input) {
        if (input == null) return null;
        String cleaned = input.trim()
                .replaceAll("\\s+", "")
                .replaceAll("[^A-Za-z0-9+/=_-]", "")
                .replace('-', '+')
                .replace('_', '/');
        if (cleaned.isEmpty()) return null;
        int padding = (4 - cleaned.length() % 4) % 4;
        cleaned += "====".substring(0, padding);
        return cleaned;
    }

    /**
     * 字符串缩写（用于日志输出）
     */
    public static String abbreviate(String s) {
        if (s == null) return "null";
        if (s.length() <= 20) return "'" + s + "'";
        return "'" + s.substring(0, 10) + "..." + s.substring(s.length() - 10) + "' (len=" + s.length() + ")";
    }
}
