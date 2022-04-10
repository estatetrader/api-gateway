package com.estatetrader.apigw.core.utils;

import com.estatetrader.util.Base64Util;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA工具类, blockSize = keySize - 11;
 */
public class RsaHelper {
    private static final Logger logger = LoggerFactory.getLogger(RsaHelper.class);

    private final RSAPublicKey     publicKey;
    private final RSAPrivateCrtKey privateKey;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public RsaHelper(String publicKey, String privateKey) {
        this(Base64Util.decode(publicKey), Base64Util.decode(privateKey));
    }

    public RsaHelper(byte[] publicKey, byte[] privateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            if (publicKey != null && publicKey.length > 0) {
                this.publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKey));
            } else {
                this.publicKey = null;
            }

            if (privateKey != null && privateKey.length > 0) {
                this.privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKey));
            } else {
                this.privateKey = null;
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public RsaHelper(String publicKey) {
        this(Base64Util.decode(publicKey));
    }

    public RsaHelper(byte[] publicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            if (publicKey != null && publicKey.length > 0) {
                this.publicKey = (RSAPublicKey)keyFactory.generatePublic(new X509EncodedKeySpec(publicKey));
            } else {
                this.publicKey = null;
            }

            this.privateKey = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] encrypt(byte[] content) {
        if (publicKey == null) {
            throw new IllegalArgumentException("public key is null.");
        }

        if (content == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return readAll(content, cipher, publicKey.getModulus().bitLength() / 8 - 11);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] decrypt(byte[] secret) {
        if (privateKey == null) {
            throw new IllegalArgumentException("private key is null.");
        }

        if (secret == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return readAll(secret, cipher, privateKey.getModulus().bitLength() / 8);
        } catch (Exception e) {
            logger.debug("rsa decrypt failed.", e);
        }
        return null;
    }

    /**
     * 使用密钥对密文文本进行解密，并返回解密后的明文文本
     *
     * @param encryptedText 使用base64编码后的密文
     * @return 返回明文文本
     */
    public String decrypt(String encryptedText) {
        byte[] decrypted = decrypt(Base64Util.decode(encryptedText));
        return decrypted != null ? new String(decrypted, StandardCharsets.UTF_8) : null;
    }

    static byte[] readAll(byte[] input, Cipher cipher, int size)
        throws IOException, IllegalBlockSizeException, BadPaddingException {

        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        int left;
        for (int i = 0; i < input.length;) {
            left = input.length - i;
            if (left > size) {
                cipher.update(input, i, size);
                i += size;
            } else {
                cipher.update(input, i, left);
                i += left;
            }

            out.write(cipher.doFinal());
        }

        return out.toByteArray();
    }

    public byte[] sign(byte[] content) {
        if (privateKey == null) {
            throw new RuntimeException("private key is null.");
        }
        if (content == null) {
            return null;
        }
        try {
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(privateKey);
            signature.update(content);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verify(byte[] sign, byte[] content) {
        if (publicKey == null) {
            throw new RuntimeException("public key is null.");
        }
        if (sign == null || content == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initVerify(publicKey);
            signature.update(content);
            return signature.verify(sign);
        } catch (Exception e) {
            logger.debug("rsa verify failed.", e);
            return false;
        }
    }

    public static byte[] encrypt(byte[] content, byte[] publicKey) {
        if (content == null || publicKey == null) {
            return null;
        }
        return new RsaHelper(publicKey, null).encrypt(content);
    }

    public static byte[] decrypt(byte[] secret, byte[] privateKey) {
        if (secret == null || privateKey == null) {
            return null;
        }
        return new RsaHelper(null, privateKey).decrypt(secret);
    }

    public static byte[] sign(byte[] content, byte[] privateKey) {
        if (content == null || privateKey == null) {
            return null;
        }
        return new RsaHelper(null, privateKey).sign(content);
    }

    public static boolean verify(byte[] sign, byte[] content, byte[] publicKey) {
        if (sign == null || content == null || publicKey == null) {
            return false;
        }
        try {
            return new RsaHelper(publicKey, null).verify(sign, content);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("exception occurred while verifying rsa sign with public key " +
                    Base64Util.encodeToString(publicKey), e);
            }
            return false;
        }
    }
}
