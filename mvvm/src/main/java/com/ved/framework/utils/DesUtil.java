package com.ved.framework.utils;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * DES 加密解密
 */
public final class DesUtil {

    private static final String ALGORITHM = "DES";

    private DesUtil() {
    }

    /**
     * DES加密
     *
     * @param base64Key : 经BASE64二次加密的密钥
     * @return
     */
    public static byte[] encryptByDES(String base64Key, byte[] dataBytes)
            throws Exception {
        // BASE64解密
        String encodeKey = Base64Util.decryptBASE64ToStr(base64Key);
        // 获得key
        Key key = getKey(encodeKey);
        // 获得加密解密对象
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        // 初始化，使用密钥进行加密
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(dataBytes);
    }

    /**
     * DES解密
     *
     * @param base64Key        ： 经过BASE64编码且SecretKey加密的密钥
     * @param encryptDataBytes ：经过加密的数据
     * @return ：译码之后的字节数组
     * @throws Exception
     */
    public static byte[] decryptByDES(String base64Key,
                                      byte[] encryptDataBytes) throws Exception {
        // 获得key
        Key k = getKey(Base64Util.decryptBASE64ToStr(base64Key));
        // 获得加密解密对象
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        // 解密
        cipher.init(Cipher.DECRYPT_MODE, k);
        return cipher.doFinal(encryptDataBytes);
    }

    /**
     * 获取解码后密钥
     *
     * @param encodeKey ： 经SecretKey加密的密钥字符串
     * @return Key ：解码后的密钥
     */
    private static Key getKey(String encodeKey) {
        SecretKey key = null;
        try {
            // 密钥参数
            DESKeySpec dks = new DESKeySpec(encodeKey.getBytes());
            SecretKeyFactory factory = SecretKeyFactory
                    .getInstance(ALGORITHM);
            // 生成密钥
            key = factory.generateSecret(dks);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return key;
    }

    /**
     * 生成密钥
     *
     * @param seed ： 种子
     * @return ：经过SecretKey一次编码与BASE64二次编码的密钥字符串
     */
    public static String initKey(String seed) {
        SecureRandom random = null;
        if (null != seed) {
            random = new SecureRandom(seed.getBytes());
        } else {
            random = new SecureRandom();
        }
        KeyGenerator generator = null;
        try {
            generator = KeyGenerator.getInstance(ALGORITHM);
            generator.init(random);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey key = generator.generateKey();
        byte[] encodeKey = key.getEncoded();
        String base64Key = Base64Util.encryptBASE64(encodeKey);
        return base64Key;
    }
}
