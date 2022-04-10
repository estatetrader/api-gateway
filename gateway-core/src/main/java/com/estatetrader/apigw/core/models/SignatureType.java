package com.estatetrader.apigw.core.models;

/**
 * 签名验证通过时使用的签名类型
 */
public enum SignatureType {
    /**
     * 使用动态盐
     */
    DYNAMIC_SALT("dynamic"),
    /**
     * 使用静态盐
     */
    STATIC_SALT("static"),
    /**
     * 使用公钥校验
     */
    PUBLIC_KEY("public-key"),
    /**
     * 未使用签名，注意应和null区分开，null表示签名校验失败
     */
    NONE("none");

    private final String description;

    SignatureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
