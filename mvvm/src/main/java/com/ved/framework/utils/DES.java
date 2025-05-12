package com.ved.framework.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 使用DES加密模式，key需要为32位
 */
public class DES {
    private static final String CHARSET = "UTF-8";
    private static final String ALGORITHM = "DESede/CBC/PKCS5Padding";
    private static final String TRANSFORMATION = "DESede";

    public static String encryptDES(String encryptString, String encryptKey, byte[] iv) {
        try {
            if (TextUtils.isEmpty(encryptString)) {
                return encryptString;
            }

            byte[] keyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
            byte[] tripleDesKey = new byte[24];
            System.arraycopy(keyBytes, 0, tripleDesKey, 0, Math.min(keyBytes.length, 24));

            IvParameterSpec zeroIv = new IvParameterSpec(iv);
            SecretKeySpec key = new SecretKeySpec(tripleDesKey, TRANSFORMATION);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
            byte[] encryptedData = cipher.doFinal(encryptString.getBytes(CHARSET));
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP);
        } catch (Exception e) {
            KLog.e("Encryption error: " + e.getMessage());
            return null;
        }
    }

    public static String decryptDES(String decryptString, String decryptKey, byte[] iv) {
        try {
            if (TextUtils.isEmpty(decryptString)) {
                return decryptString;
            }

            byte[] keyBytes = decryptKey.getBytes(StandardCharsets.UTF_8);
            byte[] tripleDesKey = new byte[24];
            System.arraycopy(keyBytes, 0, tripleDesKey, 0, Math.min(keyBytes.length, 24));

            byte[] byteMi = Base64.decode(decryptString, Base64.NO_WRAP);
            IvParameterSpec zeroIv = new IvParameterSpec(iv);
            SecretKeySpec key = new SecretKeySpec(tripleDesKey, TRANSFORMATION);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
            byte[] decryptedData = cipher.doFinal(byteMi);
            return new String(decryptedData, CHARSET);
        } catch (Exception e) {
            KLog.e("Decryption error: " + e.getMessage());
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
