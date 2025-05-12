package com.ved.framework.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.util.Arrays;

import javax.crypto.BadPaddingException;
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
            // 1. 空值检查
            if (TextUtils.isEmpty(decryptString)) {
                return decryptString;
            }

            // 2. Base64预处理
            String processedBase64 = decryptString.trim()
                    .replaceAll("[^A-Za-z0-9+/=_-]", "")
                    .replace('-', '+')
                    .replace('_', '/');

            // 3. 补全padding
            switch (processedBase64.length() % 4) {
                case 2: processedBase64 += "=="; break;
                case 3: processedBase64 += "="; break;
            }

            // 4. 解码
            byte[] byteMi = Base64.decode(processedBase64, Base64.NO_WRAP);

            // 5. 解密
            byte[] keyBytes = Arrays.copyOf(decryptKey.getBytes(CHARSET), 24);
            SecretKeySpec key = new SecretKeySpec(keyBytes, TRANSFORMATION);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] decryptedData = cipher.doFinal(byteMi);
            return new String(decryptedData, CHARSET).trim();

        } catch (IllegalArgumentException e) {
            KLog.e("Base64 decoding failed. Input: '" + decryptString +
                    "', Error: " + e.getMessage());
            return null;
        } catch (BadPaddingException e) {
            KLog.e("Decryption failed (bad padding). Key/IV mismatch?");
            return null;
        } catch (Exception e) {
            KLog.e("Decryption error: " + e.getClass().getSimpleName() +
                    " - " + e.getMessage());
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
