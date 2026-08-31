package com.ved.framework.utils;

import android.util.Base64;

import java.io.IOException;

/**
 * BASE64 编码解码
 */
public final class Base64Util {

    private static char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
    private static byte[] codes = new byte[256];

    static {
        for (int i = 0; i < 256; i++)
            codes[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++)
            codes[i] = (byte) (i - 'A');
        for (int i = 'a'; i <= 'z'; i++)
            codes[i] = (byte) (26 + i - 'a');
        for (int i = '0'; i <= '9'; i++)
            codes[i] = (byte) (52 + i - '0');
        codes['+'] = 62;
        codes['/'] = 63;
    }

    private Base64Util() {
    }

    public static char[] encode(byte[] data) {
        char[] out = new char[((data.length + 2) / 3) * 4];
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;
            int val = (0xFF & (int) data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & (int) data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & (int) data[i + 2]);
                quad = true;
            }
            out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 1] = alphabet[val & 0x3F];
            val >>= 6;
            out[index + 0] = alphabet[val & 0x3F];
        }
        return out;
    }

    public static byte[] decode(char[] data) {
        int len = ((data.length + 3) / 4) * 3;
        if (data.length > 0 && data[data.length - 1] == '=')
            --len;
        if (data.length > 1 && data[data.length - 2] == '=')
            --len;
        byte[] out = new byte[len];
        int shift = 0;
        int accum = 0;
        int index = 0;
        for (int ix = 0; ix < data.length; ix++) {
            int value = codes[data[ix] & 0xFF];
            if (value >= 0) {
                accum <<= 6;
                shift += 6;
                accum |= value;
                if (shift >= 8) {
                    shift -= 8;
                    out[index++] = (byte) ((accum >> shift) & 0xff);
                }
            }
        }
        if (index != out.length)
            throw new Error("miscalculated data length!");
        return out;
    }

    /**
     * 编码
     */
    public static String encryptBASE64(String data) {
        byte[] bytes = null;
        try {
            bytes = data.getBytes("utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * 编码
     */
    public static String encryptBASE64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static String encryptBASE64NoWrap(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * 译码
     */
    public static String decryptBASE64ToStr(String encodeStr) {
        String result = null;
        try {
            byte[] decodeBytes = Base64.decode(encodeStr, Base64.DEFAULT);
            result = new String(decodeBytes, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 译码
     */
    public static byte[] decryptBASE64ToBytes(String encodeStr) {
        return Base64.decode(encodeStr, Base64.DEFAULT);
    }
}
