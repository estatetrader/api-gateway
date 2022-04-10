package com.estatetrader.responseEntity;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

import java.io.Serializable;

/**
 * Created by rendong on 14-5-2.
 */
@Description("调用状态")
@GlobalEntityGroup
public class CallState implements Serializable {
    @Description("返回值")
    public int code;

    @Description("数据长度")
    public int length;

    @Description("返回信息")
    public String msg;
}
