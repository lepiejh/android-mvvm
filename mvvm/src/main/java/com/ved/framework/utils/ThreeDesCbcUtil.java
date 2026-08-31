package com.ved.framework.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 3DES(DESede)-CBC/PKCS5Padding 加密解密，使用 URL-safe Base64 编码。
 * <p>密钥会被自动填充/截断至 24 字节，IV 必须为 8 字节。</p>
 */
public final class ThreeDesCbcUtil {

    private static final String CHARSET = "UTF-8";
    private static final String ALGORITHM = "DESede/CBC/PKCS5Padding";
    private static final String TRANSFORMATION = "DESede";

    private static final String DEFAULT_KEY = "123456789e12345abcdefQhYJ5FHgkro";
    private static final byte[] DEFAULT_IV = {1, 2, 3, 4, 5, 6, 7, 8};

    private ThreeDesCbcUtil() {
    }

    /**
     * 3DES-CBC 加密（默认密钥）
     */
    public static String encrypt(String data) {
        return encrypt(data, DEFAULT_KEY, DEFAULT_IV);
    }

    /**
     * 3DES-CBC 加密（自定义密钥和 IV）
     */
    public static String encrypt(String data, String key, byte[] iv) {
        try {
            if (TextUtils.isEmpty(data)) return data;

            byte[] keyBytes = Arrays.copyOf(key.getBytes(CHARSET), 24);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, TRANSFORMATION);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encryptedData = cipher.doFinal(data.getBytes(CHARSET));
            return CryptoHelper.urlSafeBase64Encode(encryptedData);

        } catch (Exception e) {
            KLog.e("3DES encryption failed. Input: '" + data +
                    "', Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 3DES-CBC 解密（默认密钥）
     */
    public static String decrypt(String data) {
        return decrypt(data, DEFAULT_KEY, DEFAULT_IV);
    }

    /**
     * 3DES-CBC 解密（自定义密钥和 IV）
     */
    public static String decrypt(String data, String key, byte[] iv) {
        if (TextUtils.isEmpty(data)) {
            KLog.w("Empty input string");
            return data;
        }

        try {
            String processedBase64 = CryptoHelper.preprocessBase64(data);
            if (processedBase64 == null) {
                KLog.e("Invalid Base64 format after preprocessing");
                return null;
            }

            byte[] byteMi;
            try {
                byteMi = Base64.decode(processedBase64, Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                KLog.e("Base64 decoding failed. Processed: " + processedBase64);
                return null;
            }

            if (byteMi.length % 8 != 0) {
                KLog.e("Invalid ciphertext length: " + byteMi.length +
                        "  ,byteMi : " + Arrays.toString(byteMi));
                return null;
            }

            byte[] keyBytes = Arrays.copyOf(key.getBytes(CHARSET), 24);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, TRANSFORMATION);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decryptedData = cipher.doFinal(byteMi);
            return new String(decryptedData, CHARSET).trim();

        } catch (Exception e) {
            KLog.e("3DES decryption failed. Input: " + CryptoHelper.abbreviate(data) +
                    ", Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
}
