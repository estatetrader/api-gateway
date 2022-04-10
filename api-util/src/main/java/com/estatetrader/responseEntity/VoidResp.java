package com.estatetrader.responseEntity;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

import java.io.Serializable;

@Description("表示API无返回值")
@GlobalEntityGroup
public final class VoidResp implements Serializable {
    private static final long serialVersionUID = 1L;

    public static VoidResp convert(Object value) {
        return new VoidResp();
    }
}