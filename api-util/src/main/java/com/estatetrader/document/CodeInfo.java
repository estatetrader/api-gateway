package com.estatetrader.document;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("编码信息")
public class CodeInfo implements Serializable {
    @Description("编码值")
    public int code;
    @Description("编码名称")
    public String name;
    @Description("编码描述")
    public String desc;
    @Description("编码所属服务")
    public String service;
    @Description("是否显示给客户端")
    public boolean exposedToClient;
}