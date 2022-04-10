package com.estatetrader.apigw.core.models;

/**
 * 通用参数实体
 */
public class CommonParameterInfo {
    /**
     * 参数类型，必须以下划线开头
     */
    private final String name;
    /**
     * 参数说明
     */
    private final String desc;
    /**
     * 是否可以直接由前端指定
     */
    private final boolean fromClient;
    /**
     * 是否可用于API自动注入参数的注入
     */
    private final boolean injectable;

    public CommonParameterInfo(String name, String desc, boolean fromClient, boolean injectable) {
        this.name = name;
        this.desc = desc;
        this.fromClient = fromClient;
        this.injectable = injectable;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isFromClient() {
        return fromClient;
    }

    public boolean isInjectable() {
        return injectable;
    }
}
