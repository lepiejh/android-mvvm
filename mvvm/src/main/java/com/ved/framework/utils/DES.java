package com.ved.framework.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 使用DES加密模式，key需要为32位
 */
public class DES {

    private static byte[] iv = {1, 2, 3, 4, 5, 6, 7, 8};


    public static String encryptDES(String encryptString, String encryptKey) {
        try {
            if (TextUtils.isEmpty(encryptString)){
                return encryptString;
            }
            encryptKey = encryptKey.substring(0, 24);
            IvParameterSpec zeroIv = new IvParameterSpec(iv);
            SecretKeySpec key = new SecretKeySpec(encryptKey.getBytes(), "DESede");
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
            byte[] encryptedData = cipher.doFinal(encryptString.getBytes());
            return Base64.encodeToString(encryptedData, Base64.DEFAULT);
        } catch (Exception e) {
            KLog.e(e.getMessage());
            return null;
        }
    }

    public static String decryptDES(String decryptString, String decryptKey) {
        try {
            if (TextUtils.isEmpty(decryptString)){
                return decryptString;
            }
            decryptKey = decryptKey.substring(0, 24);
            byte[] byteMi = Base64.decode(decryptString, Base64.DEFAULT);
            IvParameterSpec zeroIv = new IvParameterSpec(iv);
            SecretKeySpec key = new SecretKeySpec(decryptKey.getBytes(), "DESede");
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
            byte decryptedData[] = cipher.doFinal(byteMi);
            return new String(decryptedData);
        } catch (Exception e) {
            KLog.e(e.getMessage());
            return null;
        }
    }

    /**
     * 使用默认的key加密
     * @param data
     * @return
     * @throws Exception
     */
    public static String encrypt(String data) {
        return encryptDES(data, "123456789e12345abcdefQhYJ5FHgkro");
    }

    /**
     * 使用默认的key解密
     * @param data
     * @return
     * @throws Exception
     */
    public static String desEncrypt(String data) {
        return decryptDES(data, "123456789e12345abcdefQhYJ5FHgkro");
    }
}
