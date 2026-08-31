package com.ved.framework.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 3DES 辅助类
 *
 * @author chenbo
 * @version $Id: ThreeDes.java,v 0.1 2016年1月29日 上午10:21:50 chenbo$Exp
 */
public final class ThreeDes {

    /**
     * DES,DESede,Blowfish 定义加密算法,可用
     */
    private static final String Algorithm = "DESede";

    /**
     * 填充方式
     */
    private static final String deAlgorithm = "DESede/ECB/NoPadding";

    private ThreeDes() {
    }

    /**
     * encryptMode 加密
     */
    public static byte[] encryptMode(byte[] keybyte, byte[] src) throws Exception {
        src = buildBodyBytes(src);
        // 填充Key
        keybyte = build3DesKey(keybyte);
        // 生成密钥
        SecretKey deskey = new SecretKeySpec(keybyte, 0, 24, Algorithm);
        // 加密
        Cipher c1 = Cipher.getInstance(deAlgorithm);
        c1.init(Cipher.ENCRYPT_MODE, deskey);
        return c1.doFinal(src);// 在单一方面的加密或解密
    }

    /**
     * decryptMode 解密
     */
    public static byte[] decryptMode(byte[] keybyte, byte[] src) throws Exception {
        byte[] busDt = null;
        // 填充Key
        keybyte = build3DesKey(keybyte);
        // 生成密钥
        SecretKey deskey = new SecretKeySpec(keybyte, 0, 24, Algorithm);
        // 解密
        Cipher c1 = Cipher.getInstance(deAlgorithm);
        c1.init(Cipher.DECRYPT_MODE, deskey);
        busDt = c1.doFinal(src);
        return busDt;
    }

    public static byte[] build3DesKey(byte[] temp) {
        if (temp == null || temp.length == 0) {
            return new byte[24];
        }
        byte[] key = new byte[24]; // 声明一个24位的字节数组，默认里面都是0
        int copyLen = Math.min(temp.length, key.length);
        System.arraycopy(temp, 0, key, 0, copyLen);
        // 补充的8字节取前8位(不足则补0), 避免 temp 长度不足8时越界
        for (int i = 0; i < 8; i++) {
            key[16 + i] = (i < temp.length) ? temp[i] : 0;
        }
        return key;
    }

    public static byte[] buildBodyBytes(byte[] body) {
        int len = body.length % 8 == 0 ? 0 : 8 - (body.length % 8);
        if (len > 0) {
            byte[] addByte = new byte[len];
            for (int i = 0; i < addByte.length; i++) {
                addByte[i] = 0x00;
            }
            body = ByteUtil.contactArray(body, addByte);
        }
        return body;
    }
}
