package com.ved.framework.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DES {
    private static final String CHARSET = "UTF-8";
    private static final String ALGORITHM = "DESede/CBC/PKCS5Padding";
    private static final String TRANSFORMATION = "DESede";

    public static String encryptDES(String encryptString, String encryptKey, byte[] iv) {
        try {
            if (TextUtils.isEmpty(encryptString)) {
                return encryptString;
            }

            byte[] keyBytes = Arrays.copyOf(encryptKey.getBytes(CHARSET), 24);
            SecretKeySpec key = new SecretKeySpec(keyBytes, TRANSFORMATION);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] encryptedData = cipher.doFinal(encryptString.getBytes(CHARSET));

            // 使用URL安全的Base64编码便于传输
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
                    .replace('+', '-')
                    .replace('/', '_')
                    .replace("=", "");

        } catch (Exception e) {
            KLog.e("Encryption failed. Input: '" + encryptString +
                    "', Error: " + e.getMessage());
            return null;
        }
    }

    public static String decryptDES(String decryptString, String decryptKey, byte[] iv) {
        try {
            if (TextUtils.isEmpty(decryptString)) {
                return decryptString;
            }

            // 1. 清理Base64字符串
            StringBuilder cleanBase64 = new StringBuilder(decryptString.trim()
                    .replaceAll("\\s+", "")
                    .replace('-', '+')
                    .replace('_', '/'));

            // 2. 补全padding
            while (cleanBase64.length() % 4 != 0) {
                cleanBase64.append("=");
            }

            // 3. 解码
            byte[] byteMi = Base64.decode(cleanBase64.toString(), Base64.NO_WRAP);

            // 剩余解密逻辑保持不变
            byte[] keyBytes = Arrays.copyOf(decryptKey.getBytes(CHARSET), 24);
            SecretKeySpec key = new SecretKeySpec(keyBytes, TRANSFORMATION);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] decryptedData = cipher.doFinal(byteMi);
            return new String(decryptedData, CHARSET).trim();

        } catch (Exception e) {
            KLog.e("Decryption failed. Input: '" + decryptString +
                    "', Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    public static String encrypt(String data) {
        return encryptDES(data, "123456789e12345abcdefQhYJ5FHgkro", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    public static String desEncrypt(String data) {
        return decryptDES(data, "123456789e12345abcdefQhYJ5FHgkro", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }
}
