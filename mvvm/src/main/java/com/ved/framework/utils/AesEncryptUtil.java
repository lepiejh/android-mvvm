package com.ved.framework.utils;

import android.text.TextUtils;
import android.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEncryptUtil {
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String CHARSET = "UTF-8";

    // Default 16-byte key and IV (128-bit AES)
    private static final String DEFAULT_KEY = "1234567890abcdef"; // Exactly 16 ASCII characters
    private static final String DEFAULT_IV = "1234567890abcdef"; // Exactly 16 ASCII characters

    public static String encrypt(String data, String key, String iv) {
        try {
            // 1. 验证输入
            if (TextUtils.isEmpty(data)) {
                return data;
            }

            // 2. 验证key和IV
            byte[] keyBytes = validateKey(key);
            byte[] ivBytes = validateIV(iv);

            // 3. 执行加密
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(CHARSET));

            // 4. 生成URL安全的Base64（无padding）
            return android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
                    .replace('+', '-')
                    .replace('/', '_')
                    .replace("=", "");

        } catch (Exception e) {
            KLog.e("Encryption failed. Input: '" + data +
                    "', Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    public static String desEncrypt(String data, String key, String iv) {
        try {
            // 1. 清理和验证输入
            if (TextUtils.isEmpty(data)) {
                return data;
            }

            // 2. Base64预处理
            String processedBase64 = data.trim()
                    .replaceAll("[^A-Za-z0-9+/=_-]", "")
                    .replace('-', '+')
                    .replace('_', '/');

            // 3. 补全padding
            switch (processedBase64.length() % 4) {
                case 2: processedBase64 += "=="; break;
                case 3: processedBase64 += "="; break;
            }

            // 4. 解码
            byte[] encryptedData = android.util.Base64.decode(processedBase64, Base64.NO_WRAP);

            // 5. 验证key和IV
            byte[] keyBytes = validateKey(key);
            byte[] ivBytes = validateIV(iv);

            // 6. 执行解密
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] original = cipher.doFinal(encryptedData);
            return new String(original, CHARSET).trim();

        } catch (IllegalArgumentException e) {
            KLog.e("Invalid Base64: " + data + ", Error: " + e.getMessage());
            return null;
        } catch (BadPaddingException e) {
            KLog.e("Decryption failed (bad padding). Key/IV mismatch?");
            return null;
        }  catch (Exception e) {
            KLog.e("Decryption failed. Input: '" + data +
                    "', Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    // Helper method to ensure 16-byte key
    private static byte[] validateKey(String key) throws Exception {
        byte[] keyBytes = key.getBytes(CHARSET);
        if (keyBytes.length != 16) {
            throw new IllegalArgumentException("Key must be exactly 16 bytes (128-bit)");
        }
        return keyBytes;
    }

    // Helper method to ensure 16-byte IV
    private static byte[] validateIV(String iv) throws Exception {
        byte[] ivBytes = iv.getBytes(CHARSET);
        if (ivBytes.length != 16) {
            throw new IllegalArgumentException("IV must be exactly 16 bytes");
        }
        return ivBytes;
    }

    public static String encrypt(String data) {
        return encrypt(data, DEFAULT_KEY, DEFAULT_IV);
    }

    public static String desEncrypt(String data) {
        return desEncrypt(data, DEFAULT_KEY, DEFAULT_IV);
    }
}
