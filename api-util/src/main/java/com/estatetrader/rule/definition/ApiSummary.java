package com.estatetrader.rule.definition;

import java.io.Serializable;

public class ApiSummary implements Serializable {
    /**
     * 所有api信息序列化之后的md5值，用于防止无意义的重复刷新
     */
    private String hash;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
