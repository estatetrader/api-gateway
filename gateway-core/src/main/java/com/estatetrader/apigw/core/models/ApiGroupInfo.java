package com.estatetrader.apigw.core.models;

import com.estatetrader.entity.AbstractReturnCode;

import java.io.Serializable;
import java.util.List;

public class ApiGroupInfo implements Serializable {
    public String name;
    public String owner;
    public int minCode;
    public int maxCode;
    public List<AbstractReturnCode> codes;
}
