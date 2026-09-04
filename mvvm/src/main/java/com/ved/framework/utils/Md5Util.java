package com.ved.framework.utils;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * MD5 单向加密，不能解密，一般用于验证密码是否正确
 */
public final class Md5Util {

    private static final String MD5 = "MD5";

    private Md5Util() {
    }

    /**
     * MD5 加密，单向加密，不能解密的
     */
    public static byte[] encryptByMD5(String data) {
        byte[] resultBytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance(MD5);
            byte[] bytes = data.getBytes("utf-8");
            resultBytes = md.digest(bytes);
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return resultBytes;
    }

    /**
     * 获取字符串的 MD5
     */
    public static String encodeStringByMD5(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes("UTF-8"));
            byte messageDigest[] = md5.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(String.format(Locale.getDefault(), "%02X", b));
            }
            return hexString.toString().toLowerCase();
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return "";
    }

    /**
     * 获取文件的 MD5
     */
    public static String encodeFileByMD5(File file) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            FileInputStream inputStream = new FileInputStream(file);
            DigestInputStream digestInputStream = new DigestInputStream(inputStream,
                    messageDigest);
            //必须把文件读取完毕才能拿到md5
            byte[] buffer = new byte[4096];
            while (digestInputStream.read(buffer) > -1) {
            }
            MessageDigest digest = digestInputStream.getMessageDigest();
            digestInputStream.close();
            byte[] md5 = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5) {
                sb.append(String.format(Locale.getDefault(), "%02X", b));
            }
            return sb.toString().toLowerCase();
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return null;
    }

    /**
     * 一般情况下，先使用MD5加密，再使用BASE64编码传输
     */
    public static String encryptByMD5AndBASE64(String data) {
        byte[] bytes = encryptByMD5(data);
        String buff = null;
        try {
            buff = new String(bytes, "utf-8");
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        String result = Base64Util.encryptBASE64(buff);
        return result;
    }
}
