package com.estatetrader.util;

import com.estatetrader.entity.CallerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 处理使用AES秘钥加密用户信息而产生的token
 *
 * @author rendong
 */
public class AESTokenHelper {
    private static final Logger logger                   = LoggerFactory.getLogger(AESTokenHelper.class);
    private static final short TOKEN_VERSION_10 = 10;
    private static final short TOKEN_VERSION_11 = 11;
    private static final short TOKEN_VERSION_12 = 12;
    private static final short TOKEN_VERSION_13 = 13;
    private static final short TOKEN_VERSION_14 = 14;
    private static final short TOKEN_VERSION_15 = 15;

    public static final String DELIMITER = "_";
    public static final String USER_TOKEN_PREFIX = "utk_";
    public static final String PARTNER_TOKEN_PREFIX = "ptk_";
    public static final String DEVICE_TOKEN_PREFIX = "dtk_";
    public static final String UNIDENTIFIED_USER_TOKEN_PREFIX = "ntk_";
    private final AesHelper aes;

    public AESTokenHelper(String pwd) {
        aes = new AesHelper(Base64Util.decode(pwd), null);
    }

    /**
     * 从base64编码的字符串中解析调用者信息
     */
    public CallerInfo parseToken(String token) {
        String trimmedToken = trimToken(token);
        if (trimmedToken == null) {
            return null;
        }

        try {
            return parseToken(Base64Util.decode(trimmedToken));
        } catch (Exception e) {
            logger.warn("token parse failed: " + token, e);
            return null;
        }
    }

    public static String trimToken(String token) {
        String[] parts = token.split(DELIMITER);
        if (parts.length != 2) {
            logger.warn("invalid token format: {}", token);
            return null;
        }

        return parts[1];
    }

    /**
     * 解析调用者信息
     */
    private CallerInfo parseToken(byte[] token) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(aes.decrypt(token)));
        try {
            CallerInfo caller = new CallerInfo();

            caller.version = dis.readShort();
            if (caller.version < TOKEN_VERSION_10) {
                throw new IOException("illegal token version: " + caller.version);
            }

            caller.expire = dis.readLong();
            caller.securityLevel = dis.readInt();
            caller.appid = dis.readInt();
            caller.deviceId = dis.readLong();
            caller.uid = dis.readLong();
            caller.key = readBytesOfShort(dis);
            caller.phoneNumber = readStringOfByte(dis);

            if (caller.version >= TOKEN_VERSION_11) {
                // from 11
                caller.role = readStringOfByte(dis);
                caller.subsystem = readStringOfByte(dis);
                caller.subSystemMainId = dis.readLong();
                caller.oauthid = readStringOfShort(dis);
            }

            if (caller.version >= TOKEN_VERSION_12) {
                // from 12
                caller.renewWindow = dis.readLong();
            }

            if (caller.version >= TOKEN_VERSION_13) {
                caller.createdTime = dis.readLong();
            }

            caller.identified = true; // 从兼容角度考虑，所有老版本的utk应均为已认证的token，如果此token不是utk，则identified无意义
            if (caller.version >= TOKEN_VERSION_14) {
                caller.identified = dis.readBoolean();
            }

            if (caller.version >= TOKEN_VERSION_15) {
                // from 15
                caller.partnerBindId = dis.readLong();
            }

            return caller;
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                logger.warn("token parse failed.close input stream failed!", e);
            }
        }
    }

    /**
     * 生成用户token
     */
    private byte[] generateToken(CallerInfo caller) {
        try {
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream(8)) {
                caller.version = TOKEN_VERSION_15;

                DataOutputStream os = new DataOutputStream(stream);
                os.writeShort(caller.version);
                os.writeLong(caller.expire);
                os.writeInt(caller.securityLevel);
                os.writeInt(caller.appid);
                os.writeLong(caller.deviceId);
                os.writeLong(caller.uid);

                writeBytesOfShort(os, caller.key);
                writeStringOfByte(os, caller.phoneNumber);
                writeStringOfByte(os, caller.role);
                writeStringOfByte(os, caller.subsystem);
                os.writeLong(caller.subSystemMainId);
                writeStringOfShort(os, caller.oauthid);
                os.writeLong(caller.renewWindow);
                // 强制使用当前时间，而不是caller中提供的时间
                os.writeLong(System.currentTimeMillis());
                os.writeBoolean(caller.identified);
                os.writeLong(caller.partnerBindId);

                return aes.encrypt(stream.toByteArray());
            }
        } catch (IOException e) {
            throw new IllegalStateException("generator token failed.", e);
        }
    }

    /**
     * 生成用户token
     */
    public String generateStringUserToken(CallerInfo caller) {
        String token = Base64Util.encodeToString(this.generateToken(caller));
        return USER_TOKEN_PREFIX + token;
    }

    /**
     * 生成三方绑定token
     */
    public String generateStringPartnerToken(CallerInfo caller) {
        String token = Base64Util.encodeToString(this.generateToken(caller));
        return PARTNER_TOKEN_PREFIX + token;
    }

    /**
     * 生成未认证的用户token(ntk)
     */
    public String generateStringUnidentifiedUserToken(CallerInfo caller) {
        caller.identified = false;
        String token = Base64Util.encodeToString(this.generateToken(caller));
        return UNIDENTIFIED_USER_TOKEN_PREFIX + token;
    }

    /**
     * 生成设备token
     */
    public String generateStringDeviceToken(CallerInfo caller) {
        caller.uid = 0;
        return DEVICE_TOKEN_PREFIX + Base64Util.encodeToString(this.generateToken(caller));
    }

    private byte[] readBytesOfShort(DataInputStream is) throws IOException {
        return readBytes(is.readShort(), is);
    }

    private byte[] readBytes(int len, DataInputStream is) throws IOException {
        if (len <= 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        int actual = is.read(bytes);
        if (len != actual) {
            throw new IOException("bytes read " + actual + " is not equals to the expect length " + len);
        }
        return bytes;
    }

    private String readStringOfShort(DataInputStream is) throws IOException {
        return readString(is.readShort(), is);
    }

    private String readStringOfByte(DataInputStream is) throws IOException {
        return readString(is.readByte(), is);
    }

    private String readString(int len, DataInputStream is) throws IOException {
        byte[] bytes = readBytes(len, is);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeBytesOfShort(DataOutputStream os, byte[] bytes) throws IOException {
        if (bytes == null) {
            os.writeShort(0);
        } else {
            os.writeShort(bytes.length);
            os.write(bytes);
        }
    }

    private void writeBytesOfByte(DataOutputStream os, byte[] bytes) throws IOException {
        if (bytes == null) {
            os.writeByte(0);
        } else {
            os.writeByte(bytes.length);
            os.write(bytes);
        }
    }

    private void writeStringOfShort(DataOutputStream os, String s) throws IOException {
        writeBytesOfShort(os, s == null ? null : s.getBytes(StandardCharsets.UTF_8));
    }

    private void writeStringOfByte(DataOutputStream os, String s) throws IOException {
        writeBytesOfByte(os, s == null ? null : s.getBytes(StandardCharsets.UTF_8));
    }
}
