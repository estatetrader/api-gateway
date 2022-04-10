package com.estatetrader.responseEntity;

import java.io.Serializable;
import java.util.Date;

import com.estatetrader.util.DateUtil;
import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

/**
 * Created by rendong on 14-4-25.
 */
@Description("字符串返回值")
@GlobalEntityGroup
public final class StringResp implements Serializable {
    private static final long serialVersionUID = 1L;
    @Description("字符串返回值")
    public String value;

    public static StringResp convert(String s) {
        StringResp sr = new StringResp();
        sr.value = s;
        return sr;
    }
    
    public static StringResp convert(Date d) {
        StringResp sr = new StringResp();
        if (d != null) {
            sr.value = DateUtil.toString(d);
        }
        return sr;
    }
}
