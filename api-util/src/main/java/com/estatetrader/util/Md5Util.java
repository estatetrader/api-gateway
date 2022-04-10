package com.estatetrader.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    public static byte[] compute(byte[] content) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(content);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String computeToHex(byte[] content) {
        return HexStringUtil.toHexString(compute(content));
    }

    public static String computeToHexBit16(byte[] content) {
        return HexStringUtil.toHexString(compute(content)).substring(8, 24);
    }

    public static String computeToBase64(byte[] content) {
        return Base64Util.encodeToString(compute(content));
    }
}
