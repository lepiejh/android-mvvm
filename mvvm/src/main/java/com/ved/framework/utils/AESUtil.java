package com.ved.framework.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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

    private static final IvParameterSpec enc_iv;
    private static SecretKeySpec enc_key;
    private static Cipher cipherEnc;

    //密钥有限制，貌似不能随便改
    private static final String AESKEY = "0123456789abcdef";

    static final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    static {//初始化代码块
        try {
            cipherEnc = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            KLog.e(ex.getMessage());
        }
        enc_key = new SecretKeySpec(AESKEY.getBytes(), "AES");
        String ivKey = "fedcba9876543210";
        enc_iv = new IvParameterSpec(ivKey.getBytes());
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
            enc_key = getKey();
            cipherEnc.init(Cipher.ENCRYPT_MODE, enc_key, enc_iv);
            String padRight = padRight(str, ((int) Math.ceil(str.length() / 16.0)) * 16);
            byte[] origData = padRight.getBytes(StandardCharsets.UTF_8);
            ret = cipherEnc.doFinal(origData);
        } catch (Exception ex) {
            KLog.e(ex.getMessage());
            return null;
        }
        return byteArray2HexString(ret);
    }

    /**
     * 解密
     *
     * @param str 要解密的字符串
     */
    public static String decrypt(String str) {
        byte[] ret = null;
        try {
            cipherEnc.init(Cipher.DECRYPT_MODE, enc_key, enc_iv);
            ret = cipherEnc.doFinal(hexString2ByteArray(str));
        } catch (Exception ex) {
            KLog.e(ex.getMessage());
            return null;
        }
        return new String(ret, StandardCharsets.UTF_8);
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
        if (hexStr.isEmpty())
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
    public static String parseByte2HexStr(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase(Locale.getDefault()));
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
    private static SecretKeySpec getKey() throws Exception {
        byte[] arrBTmp = AESUtil.AESKEY.getBytes();
        byte[] arrB = new byte[16]; // 创建一个空的16位字节数组（默认值为0）
        for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {
            arrB[i] = arrBTmp[i];
        }
        return new SecretKeySpec(arrB, "AES");
    }

    /**
     * 解密 encryptString2Bytes 加密后的字节数组
     *
     * @param encrypted 加密后的字节数组（前 16 字节是 IV，后面是密文）
     * @return 解密后的字符串；失败返回 null
     */
    public static String decryptStringFromBytes(byte[] encrypted) {
        if (encrypted == null || encrypted.length <= 16) {
            KLog.e("解密失败：密文为空或长度不足");
            return null;
        }
        try {
            // 1. 取出前 16 字节作为 IV
            byte[] iv = new byte[16];
            System.arraycopy(encrypted, 0, iv, 0, 16);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 2. 剩下的才是真正的密文
            byte[] cipherText = new byte[encrypted.length - 16];
            System.arraycopy(encrypted, 16, cipherText, 0, cipherText.length);

            // 3. 用同一把密钥解密（注意：必须和加密时用的 key 完全一致）
            SecretKeySpec key = new SecretKeySpec(getKeyBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] decrypted = cipher.doFinal(cipherText);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return null;
    }

    /**
     * 获取加密用的 16 字节密钥（和 getKey() 保持一致）
     */
    private static byte[] getKeyBytes() {
        byte[] arrBTmp = AESUtil.AESKEY.getBytes(StandardCharsets.UTF_8);
        byte[] arrB = new byte[16]; // 默认填充 0
        for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {
            arrB[i] = arrBTmp[i];
        }
        return arrB;
    }

    /**
     * 加密
     *
     * @param content 需要加密的内容
     * @return 解密后的字节数组
     */
    public static byte[] encryptString2Bytes(String content) {
        try {
            // 用固定密钥，而不是每次随机生成
            SecretKeySpec key = new SecretKeySpec(getKeyBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // 随机生成 16 字节 IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(byteContent);

            // IV 拼在密文前面
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;

        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return null;
    }
}
