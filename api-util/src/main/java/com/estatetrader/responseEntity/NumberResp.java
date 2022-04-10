package com.estatetrader.responseEntity;

import java.io.Serializable;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

/**
 * Created by rendong on 14-4-25.
 */
@Description("数值型返回值，包含byte, char, short, int")
@GlobalEntityGroup
public final class NumberResp implements Serializable {
    private static final long serialVersionUID = 1L;

    @Description("数值型返回值，包含byte, char, short, int")
    public int value;

    public static NumberResp convert(Byte n) {
        NumberResp nr = new NumberResp();
        nr.value = n == null ? 0 : n;
        return nr;
    }

    public static NumberResp convert(Character n) {
        NumberResp nr = new NumberResp();
        nr.value = n == null ? 0 : n;
        return nr;
    }

    public static NumberResp convert(Short n) {
        NumberResp nr = new NumberResp();
        nr.value = n == null ? 0 : n;
        return nr;
    }

    public static NumberResp convert(Integer n) {
        NumberResp nr = new NumberResp();
        nr.value = n == null ? 0 : n;
        return nr;
    }
}
