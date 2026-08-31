package com.ved.framework.utils;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

/**
 * RSA 非对称加密解密
 *
 * @author Administrator
 */
public final class RsaUtil {

    public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    private static final String PUBLIC_KEY = "RSAPublicKey";
    private static final String PRIVATE_KEY = "RSAPrivateKey";

    private static final int KEY_LENGTH = 1024;

    private RsaUtil() {
    }

    /**
     * 使用私钥对数据进行加密
     */
    public static byte[] encryptByPrivateKey(byte[] data, String base64Key)
            throws Exception {
        byte[] keyBytes = Base64Util.decryptBASE64ToBytes(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateKey = factory.generatePrivate(spec);
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    /**
     * 使用公钥对数据进行加密
     */
    public static byte[] encryptByPublicKey(byte[] data, String base64Key)
            throws Exception {
        byte[] keyBytes = Base64Util.decryptBASE64ToBytes(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey publicKey = factory.generatePublic(spec);
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * 使用公钥解密数据
     */
    public static byte[] decryptByPublicKey(byte[] data, String base64Key)
            throws Exception {
        byte[] keyBytes = Base64Util.decryptBASE64ToBytes(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey publicKey = factory.generatePublic(spec);
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * 使用私钥解密数据
     */
    public static byte[] decryptByPrivateKey(byte[] data, String base64Key)
            throws Exception {
        byte[] keyBytes = Base64Util.decryptBASE64ToBytes(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateKey = factory.generatePrivate(spec);
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    /**
     * 初始化密钥对，将生成的密钥对存放到Map集合
     */
    public static Map<String, Object> initKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator
                .getInstance(KEY_ALGORITHM);
        generator.initialize(KEY_LENGTH);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        Map<String, Object> keyMap = new HashMap<String, Object>(2);
        keyMap.put(PRIVATE_KEY, privateKey);
        keyMap.put(PUBLIC_KEY, publicKey);
        return keyMap;
    }

    /**
     * 获得BASE64编码的私钥
     */
    public static String getBase64PrivateKey(Map<String, Object> keyMap) {
        Key privateKey = (Key) keyMap.get(PRIVATE_KEY);
        String base64PrivateKey = Base64Util.encryptBASE64(privateKey.getEncoded());
        return base64PrivateKey;
    }

    /**
     * 获得BASE64编码的公钥
     */
    public static String getBase64PublicKey(Map<String, Object> keyMap) {
        Key publicKey = (Key) keyMap.get(PUBLIC_KEY);
        String base64PrivateKey = Base64Util.encryptBASE64(publicKey.getEncoded());
        return base64PrivateKey;
    }

    /**
     * 用私钥对信息生成数字签名
     */
    public static String sign(byte[] data, String privateKey)
            throws Exception {
        byte[] keyBytes = Base64Util.decryptBASE64ToBytes(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(priKey);
        signature.update(data);
        return Base64Util.encryptBASE64(signature.sign());
    }

    /**
     * 校验数字签名
     *
     * @return 校验成功返回true 失败返回false
     */
    public static boolean verify(byte[] data, String publicKey, String sign)
            throws Exception {
        byte[] keyBytes = Base64Util.decryptBASE64ToBytes(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(pubKey);
        signature.update(data);
        return signature.verify(Base64Util.decryptBASE64ToBytes(sign));
    }
}
