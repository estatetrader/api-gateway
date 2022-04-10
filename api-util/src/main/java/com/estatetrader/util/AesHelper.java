package com.estatetrader.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AesHelper {
    private final SecretKeySpec   keySpec;
    private final IvParameterSpec iv;
    // 需要使用无填充时使用，此时会为密钥计算出一个唯一的iv来使用
    private final boolean useCFB;

    public AesHelper(byte[] aesKey, byte[] iv) {
        if (aesKey == null || aesKey.length < 16 || (iv != null && iv.length < 16)) {
            throw new IllegalArgumentException("错误的初始密钥");
        }
        if (iv == null) {
            iv = Md5Util.compute(aesKey);
        }
        this.useCFB = false;
        keySpec = new SecretKeySpec(aesKey, "AES");
        this.iv = new IvParameterSpec(iv);
    }

    public AesHelper(byte[] aesKey, boolean cfb) {
        if (aesKey == null || aesKey.length < 16) {
            throw new IllegalArgumentException("错误的初始密钥");
        }
        useCFB = cfb;
        keySpec = new SecretKeySpec(aesKey, "AES");
        this.iv = new IvParameterSpec(Md5Util.compute(aesKey));
    }

    public byte[] encrypt(byte[] data) {
        Cipher cipher;
        try {
            if (useCFB) {
                cipher = Cipher.getInstance("AES/CFB/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            } else {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            }

            return cipher.doFinal(data);
        } catch (InvalidKeyException
            | InvalidAlgorithmParameterException
            | IllegalBlockSizeException
            | BadPaddingException
            | NoSuchAlgorithmException
            | NoSuchPaddingException e) {

            throw new IllegalStateException(e);
        }
    }

    public byte[] decrypt(byte[] secret) {
        Cipher cipher;
        try {
            if (useCFB) {
                cipher = Cipher.getInstance("AES/CFB/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            } else {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            }

            return cipher.doFinal(secret);
        } catch (InvalidKeyException
            | InvalidAlgorithmParameterException
            | IllegalBlockSizeException
            | BadPaddingException
            | NoSuchAlgorithmException
            | NoSuchPaddingException e) {

            throw new IllegalStateException(e);
        }
    }

    /**
     * 对明文进行加密，并将加密结果进行base64编码
     * @param text 需要加密的明文
     * @return base64编码之后的加密结果
     */
    public String encrypt(String text) {
        return encrypt(text, Base64Util.NO_WRAP);
    }

    /**
     * 对明文进行加密，并将加密结果进行base64编码
     * @param text 需要加密的明文
     * @param base64Flags base64编码使用的标志位
     * @return base64编码之后的加密结果
     */
    public String encrypt(String text, int base64Flags) {
        return Base64Util.encodeToString(encrypt(text.getBytes()), base64Flags);
    }

    /**
     * 对密文进行解密，并返回其明文
     * 注意：传入的文本须是经过base64编码处理后的密文
     *
     * @param encryptedText base64编码后的密文
     * @return 返回密文对应的明文，字符串格式
     */
    public String decrypt(String encryptedText) {
        return decrypt(encryptedText, Base64Util.NO_WRAP);
    }

    /**
     * 对密文进行解密，并返回其明文
     * 注意：传入的文本须是经过base64编码处理后的密文
     *
     * @param encryptedText base64编码后的密文
     * @param base64Flags base64编码时使用的标志位，注意，此标志位须和编码时的标志位一致
     * @return 返回密文对应的明文，字符串格式
     */
    public String decrypt(String encryptedText, int base64Flags) {
        return new String(decrypt(Base64Util.decode(encryptedText, base64Flags)));
    }

    public static byte[] randomKey(int size) {
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(size, new SecureRandom());
            return gen.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
