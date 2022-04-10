package com.estatetrader.document;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("通用参数")
public class CommonParameterInfo implements Serializable {
    @Description("参数名称")
    public String name;

    @Description("描述")
    public String desc;

    @Description("是否为客户端指定")
    public boolean fromClient;

    @Description("是否能用于API参数注入")
    public boolean injectable;
}