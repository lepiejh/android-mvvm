package com.ved.framework.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-CBC/PKCS5Padding 加密解密，使用 URL-safe Base64 编码。
 * <p>密钥和 IV 均必须为 16 字节（128-bit）。</p>
 */
public final class AesCbcUtil {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String CHARSET = "UTF-8";

    private static final String DEFAULT_KEY = "1234567890abcdef";
    private static final String DEFAULT_IV = "1234567890abcdef";

    private AesCbcUtil() {
    }

    /**
     * AES-CBC 加密（默认密钥）
     */
    public static String encrypt(String data) {
        return encrypt(data, DEFAULT_KEY, DEFAULT_IV);
    }

    /**
     * AES-CBC 加密（自定义密钥和 IV）
     */
    public static String encrypt(String data, String key, String iv) {
        try {
            if (TextUtils.isEmpty(data)) return data;

            byte[] keyBytes = validateKey(key);
            byte[] ivBytes = validateIV(iv);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(CHARSET));
            return CryptoHelper.urlSafeBase64Encode(encrypted);

        } catch (Exception e) {
            KLog.e("AES encryption failed. Input: '" + data +
                    "', Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * AES-CBC 解密（默认密钥）
     */
    public static String decrypt(String data) {
        return decrypt(data, DEFAULT_KEY, DEFAULT_IV);
    }

    /**
     * AES-CBC 解密（自定义密钥和 IV）
     */
    public static String decrypt(String data, String key, String iv) {
        try {
            if (TextUtils.isEmpty(data)) {
                KLog.w("Empty input string");
                return data;
            }

            String processedBase64 = CryptoHelper.preprocessBase64(data);
            if (processedBase64 == null) {
                KLog.e("Invalid Base64 format after preprocessing");
                return null;
            }

            byte[] encryptedData;
            try {
                encryptedData = Base64.decode(processedBase64, Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                KLog.e("Base64 decode failed. Processed: " + processedBase64);
                return null;
            }

            if (encryptedData.length % 16 != 0) {
                KLog.e("Invalid ciphertext length: " + encryptedData.length +
                        " ,encryptedData : " + Arrays.toString(encryptedData));
                return null;
            }

            byte[] keyBytes = validateKey(key);
            byte[] ivBytes = validateIV(iv);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] original = cipher.doFinal(encryptedData);
            return new String(original, CHARSET).trim();

        } catch (javax.crypto.BadPaddingException e) {
            KLog.e("Key/IV mismatch or corrupted data");
            return null;
        } catch (javax.crypto.IllegalBlockSizeException e) {
            KLog.e("Invalid block size. Possible causes: " +
                    "1. Not encrypted data, 2. Corrupted data, 3. Wrong algorithm");
            return null;
        } catch (Exception e) {
            KLog.e("AES decryption failed. Error: " + e.getClass().getSimpleName() +
                    ": " + e.getMessage());
            return null;
        }
    }

    private static byte[] validateKey(String key) throws Exception {
        byte[] keyBytes = key.getBytes(CHARSET);
        if (keyBytes.length != 16) {
            throw new IllegalArgumentException("Key must be exactly 16 bytes (128-bit)");
        }
        return keyBytes;
    }

    private static byte[] validateIV(String iv) throws Exception {
        byte[] ivBytes = iv.getBytes(CHARSET);
        if (ivBytes.length != 16) {
            throw new IllegalArgumentException("IV must be exactly 16 bytes");
        }
        return ivBytes;
    }
}
