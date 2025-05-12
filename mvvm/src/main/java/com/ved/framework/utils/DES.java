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

            // Ensure key is exactly 24 bytes (for Triple DES)
            byte[] keyBytes = Arrays.copyOf(encryptKey.getBytes(CHARSET), 24);
            SecretKeySpec key = new SecretKeySpec(keyBytes, TRANSFORMATION);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] encryptedData = cipher.doFinal(encryptString.getBytes(CHARSET));
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP);
        } catch (Exception e) {
            KLog.e("encryptDES encryptString : "+encryptString+" ,Encryption error: " + e.getMessage());
            return null;
        }
    }

    public static String decryptDES(String decryptString, String decryptKey, byte[] iv) {
        try {
            if (TextUtils.isEmpty(decryptString)) {
                return decryptString;
            }

            // Ensure key is exactly 24 bytes (for Triple DES)
            byte[] keyBytes = Arrays.copyOf(decryptKey.getBytes(CHARSET), 24);
            SecretKeySpec key = new SecretKeySpec(keyBytes, TRANSFORMATION);

            byte[] byteMi = Base64.decode(decryptString, Base64.NO_WRAP);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] decryptedData = cipher.doFinal(byteMi);
            return new String(decryptedData, CHARSET).trim();
        } catch (Exception e) {
            KLog.e("decryptDES decryptString : "+decryptString+" ,Decryption error: " + e.getMessage());
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
