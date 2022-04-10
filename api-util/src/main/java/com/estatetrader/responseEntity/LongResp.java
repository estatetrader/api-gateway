package com.estatetrader.responseEntity;

import java.io.Serializable;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

/**
 * Created by rendong on 14-4-25.
 */
@Description("长整形返回值")
@GlobalEntityGroup
public final class LongResp implements Serializable {
    private static final long serialVersionUID = 1L;

    @Description("长整形返回值")
    public long value;

    public static LongResp convert(Long l) {
        LongResp lr = new LongResp();
        lr.value = l == null ? 0 : l;
        return lr;
    }
}
