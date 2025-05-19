package com.base.log;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {

    public static final String CHARSET_NAME = "utf-8";
    public static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    public static final String AES = "AES";
    public static final String mKey = "AnTVv1Z5izJBYZSf";

    /**
     * 提供密钥和向量进行加密
     *
     * @param sSrc
     * @param key
     * @param iv
     * @return
     * @throws Exception
     */
    public static String encrypt(String sSrc, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, AES);
        Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);// "算法/模式/补码方式"
        IvParameterSpec parameterSpec = new IvParameterSpec(iv);// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, parameterSpec);
        byte[] encrypted = cipher.doFinal(sSrc.getBytes(CHARSET_NAME));
        return Base64.encodeToString(encrypted,Base64.NO_WRAP);
//        return Base64.encodeBase64String(encrypted);
    }

    /**
     * 提供密钥和向量进行解密
     *
     * @param sSrc
     * @param key
     * @param iv
     * @return
     * @throws Exception
     */
    public static String decrypt(String sSrc, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, AES);
        Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);
//        byte[] encrypted = Base64.decodeBase64(sSrc);
        byte[] encrypted =Base64.decode(sSrc,Base64.DEFAULT);
        byte[] original = cipher.doFinal(encrypted);
        return new String(original, CHARSET_NAME);
    }

    /**
     * 使用密钥进行加密
     *
     * @param sSrc
     * @param keyStr
     * @return
     * @throws Exception
     */
    public static String encrypt(String sSrc, String keyStr) {
        try {
            return encrypt(sSrc, keyStr.getBytes(CHARSET_NAME), keyStr.getBytes(CHARSET_NAME));
        } catch (Exception e) {
            System.out.println("AESUtils"+e.getMessage());
        }
        return "";

    }

    /**
     * 使用密钥进行解密
     *
     * @param sSrc
     * @param keyStr
     * @return
     * @throws Exception
     */
    public static String decrypt(String sSrc, String keyStr) {

        try {
            return decrypt(sSrc, keyStr.getBytes(CHARSET_NAME), keyStr.getBytes(CHARSET_NAME));
        } catch (Exception ignore) {
        }
        return "";
    }

    public static String encrypt(String sSrc){
        try{
            return encrypt(sSrc,mKey);
        }catch (Exception ignore){
        }
        return "";

    }

    public static String decrypt(String sSrc){
        return decrypt(sSrc,mKey);
    }

}
