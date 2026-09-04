package com.ved.framework.utils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 加密解密
 */
public final class AESUtil {

    private static IvParameterSpec enc_iv;
    private static SecretKeySpec enc_key;
    private static Cipher cipherEnc;

    //密钥有限制，貌似不能随便改
    private static String AESKEY = "0123456789abcdef";
    private static String IvKey = "fedcba9876543210";

    static final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    static {//初始化代码块
        try {
            cipherEnc = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (NoSuchPaddingException ex) {
            ex.printStackTrace();
        }
        enc_key = new SecretKeySpec(AESKEY.getBytes(), "AES");
        enc_iv = new IvParameterSpec(IvKey.getBytes());
    }

    private AESUtil() {
    }

    /**
     * 加密
     *
     * @param str 要加密的字符串
     * @return 加密后的字符串
     */
    public static String encrypt(String str) {
        byte[] ret = null;
        try {
            enc_key = getKey(AESKEY);
            cipherEnc.init(Cipher.ENCRYPT_MODE, enc_key, enc_iv);
            String padRight = padRight(str, ((int) Math.ceil(str.length() / 16.0)) * 16);
            byte[] origData = padRight.getBytes("UTF-8");
            ret = cipherEnc.doFinal(origData);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return byteArray2HexString(ret);
    }

    /**
     * 解密
     *
     * @param str 要解密的字符串
     * @return
     */
    public static String decrypt(String str) {
        byte[] ret = null;
        try {
            cipherEnc.init(Cipher.DECRYPT_MODE, enc_key, enc_iv);
            ret = cipherEnc.doFinal(hexString2ByteArray(str));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        try {
            return new String(ret, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String byteArray2HexString(byte[] b) {
        if (b == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * b.length);
        for (final byte by : b) {
            hex.append(HEX_CHAR_TABLE[(by & 0xF0) >> 4]).append(HEX_CHAR_TABLE[(by & 0x0F)]);
        }
        return hex.toString();
    }

    public static byte[] hexString2ByteArray(String s) {
        if (s == null) {
            return null;
        }
        byte high, low;
        int len = s.length() / 2;
        byte[] b = new byte[len];
        for (int i = 0, k = 0; i < len; i++, k += 2) {
            high = (byte) (Character.digit(s.charAt(k), 16) & 0x0F);
            low = (byte) (Character.digit(s.charAt(k + 1), 16) & 0x0F);
            b[i] = (byte) ((high << 4) | low);
        }
        return b;
    }

    /**
     * 将16进制转换为二进制
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 将二进制转换成16进制
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    public static String padLeft(String s, int n) {
        return String.format(Locale.getDefault(), "%1$#" + n + "s", s);
    }

    public static String padRight(String s, int n) {
        return String.format(Locale.getDefault(), "%1$-" + n + "s", s);
    }

    /**
     * 获取适配密钥
     */
    private static SecretKeySpec getKey(String strKey) throws Exception {
        byte[] arrBTmp = strKey.getBytes();
        byte[] arrB = new byte[16]; // 创建一个空的16位字节数组（默认值为0）
        for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {
            arrB[i] = arrBTmp[i];
        }
        SecretKeySpec skeySpec = new SecretKeySpec(arrB, "AES");
        return skeySpec;
    }

    /**
     * 加密
     *
     * @param content 需要加密的内容
     * @return 解密后的字节数组
     */
    public static byte[] encryptString2Bytes(String content) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128, new SecureRandom(AESKEY.getBytes()));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");// 创建密码器
            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
            byte[] result = cipher.doFinal(byteContent);
            return result; // 加密
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return null;
    }
}
