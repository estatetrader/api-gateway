package com.estatetrader.responseEntity;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

import java.io.Serializable;
import java.util.List;

@Description("授权结果")
@GlobalEntityGroup
public class AuthenticationResult implements Serializable {

    @Description("授权访问的用户id")
    public long authorizedUserId;

    @Description("授权访问的接口列表")
    public List<String> apis;
}
