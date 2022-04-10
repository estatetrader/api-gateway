package com.estatetrader.apigw.core.utils;

import com.alibaba.fastjson.JSONObject;
import com.estatetrader.entity.ExtensionCallerInfo;
import com.estatetrader.util.Base64Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Created by steven on 19/06/2017.
 */
public class RsaExtensionTokenHelper {
    private static final Logger logger                   = LoggerFactory.getLogger(RsaExtensionTokenHelper.class);
    public  static final String DELIMITER                = ".";

    private final RsaHelper rsa;

    public RsaExtensionTokenHelper(RsaHelper rsa) {
        this.rsa = rsa;
    }

    public RsaExtensionTokenHelper(String publicKey) {
        this(new RsaHelper(publicKey));
    }

    public static ExtensionCallerInfo parseToken(String token) {
        int index = token.indexOf(DELIMITER);
        if (index != -1) {
            try {
                String callerInfo = token.substring(0, index);
                byte[] content = Base64Util.decode(callerInfo);
                String json = new String(content, StandardCharsets.UTF_8);

                return JSONObject.parseObject(json, ExtensionCallerInfo.class);
            } catch (Exception e) {
                logger.error("extension token parse failed: " + token, e);
            }
        }

        return null;
    }

    public String generateToken(ExtensionCallerInfo caller) {
        try {
            String json = JSONObject.toJSONString(caller);
            byte[] content = json.getBytes(StandardCharsets.UTF_8);
            byte[] sig = rsa.sign(content);

            return Base64Util.encodeToString(content) + DELIMITER + Base64Util.encodeToString(sig);
        } catch (Exception e) {
            logger.error("generate extension token failed.", e);
        }

        return null;
    }

    public static String generateToken(ExtensionCallerInfo caller, byte[] privateKey) {
        try {
            String json = JSONObject.toJSONString(caller);
            byte[] content = json.getBytes(StandardCharsets.UTF_8);
            byte[] sig = RsaHelper.sign(content, privateKey);

            return Base64Util.encodeToString(content) + DELIMITER + Base64Util.encodeToString(sig);
        } catch (Exception e) {
            logger.error("generate extension token failed.", e);
        }

        return null;
    }

    public static String generateToken(ExtensionCallerInfo caller, String privateKey) {
        return generateToken(caller, Base64Util.decode(privateKey));
    }

    public boolean verifyToken(String token) {
        int index = token.indexOf(DELIMITER);
        if (index != -1 && index < token.length()) {
            try {
                byte[] content = Base64Util.decode(token.substring(0, index));
                byte[] sign = Base64Util.decode(token.substring(index + 1, token.length()));

                return rsa.verify(sign, content);
            } catch (Exception e) {
                logger.error("verify extension token failed: " + token, e);
            }
        }

        return false;
    }

}
