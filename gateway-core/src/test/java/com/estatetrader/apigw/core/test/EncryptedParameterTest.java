package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.ApiParameter;
import com.estatetrader.define.ApiParameterEncryptionMethod;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.util.AesHelper;
import com.estatetrader.annotation.ApiGroup;
import com.estatetrader.annotation.HttpApi;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EncryptedParameterTest extends BaseHttpTest {

    static class ReturnCode extends AbstractReturnCode {
        public ReturnCode(String desc, int code) {
            super(desc, code);
        }
    }

    @ApiGroup(name = "test", minCode = 1, maxCode = 2, codeDefine = ReturnCode.class, owner = "nick")
    public static class TestService {
        @HttpApi(name = "test.testAES", desc = "test", security = SecurityType.Anonym, owner = "nick")
        public String testAES(
            @ApiParameter(
                name = "secret",
                desc = "secret",
                required = true,
                encryptionMethod = ApiParameterEncryptionMethod.AES)
                String secret) {
            return "secret: " + secret;
        }

        @HttpApi(name = "test.testRSA", desc = "test", security = SecurityType.Anonym, owner = "nick")
        public String testRSA(
            @ApiParameter(
                name = "secret",
                desc = "secret",
                required = true,
                encryptionMethod = ApiParameterEncryptionMethod.RSA)
                String secret) {
            return "secret: " + secret;
        }
    }

    private String aesEncryptWithDefaultKey(String text) {
        byte[] key = Base64.getDecoder().decode(getProperty("gateway.default-parameter-encryption-key"));
        return aesEncrypt(key, text);
    }

    private String aesEncrypt(byte[] key, String text) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec iv = new IvParameterSpec(MessageDigest.getInstance("MD5").digest(key));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getEncoder().encode(encrypted), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException
            | NoSuchPaddingException
            | InvalidKeyException
            | InvalidAlgorithmParameterException
            | BadPaddingException
            | IllegalBlockSizeException e) {

            throw new IllegalStateException(e);
        }
    }

    private String rsaEncrypt(String text) {
        byte[] encrypted = getDefaultRSAHelper().encrypt(text.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getEncoder().encode(encrypted), StandardCharsets.UTF_8);
    }

    @Test
    public void testAESWithDefaultKey() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test.testAES");
        params.put("secret", aesEncryptWithDefaultKey("abc"));
        String result = extractResult(executeRequest(params, TestService.class));
        assertEquals("secret: abc", result);
    }

    @Test
    public void testAESWithDeviceSecret() {
        CallerInfo caller = new CallerInfo();
        caller.key = AesHelper.randomKey(256);

        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test.testAES");
        params.put("_tk", getAESTokenHelper().generateStringDeviceToken(caller));
        params.put("secret", aesEncrypt(caller.key, "abc"));
        String result = extractResult(executeRequest(params, TestService.class));
        assertEquals("secret: abc", result);
    }

    @Test
    public void testRSA() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test.testRSA");
        params.put("secret", rsaEncrypt("abc"));
        String result = extractResult(executeRequest(params, TestService.class));
        assertEquals("secret: abc", result);
    }
}
