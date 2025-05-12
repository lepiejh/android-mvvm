package com.ved.framework.utils;

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
            // Validate key and IV length
            byte[] keyBytes = validateKey(key);
            byte[] ivBytes = validateIV(iv);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(CHARSET));
            return android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            KLog.e("Encryption error: " + e.getMessage());
            return null;
        }
    }

    public static String desEncrypt(String data, String key, String iv) {
        try {
            // Validate key and IV length
            byte[] keyBytes = validateKey(key);
            byte[] ivBytes = validateIV(iv);

            byte[] encryptedData = android.util.Base64.decode(data, android.util.Base64.NO_WRAP);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] original = cipher.doFinal(encryptedData);
            return new String(original, CHARSET).trim();
        } catch (Exception e) {
            KLog.e("Decryption error: " + e.getMessage());
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
