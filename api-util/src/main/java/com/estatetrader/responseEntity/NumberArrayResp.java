package com.estatetrader.responseEntity;

import java.io.Serializable;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

/**
 * Created by rendong on 14-4-25.
 */
@Description("数值型数组返回值，包含byte, char, short, int")
@GlobalEntityGroup
public final class NumberArrayResp implements Serializable {
    private static final long serialVersionUID = 1L;

    @Description("数值型数组返回值，包含byte, char, short, int")
    public int[] value;

    public static NumberArrayResp convert(byte[] bs) {
        NumberArrayResp na = new NumberArrayResp();
        if (bs != null) {
            na.value = new int[bs.length];
            for (int i = 0; i < bs.length; i++) {
                na.value[i] = bs[i];
            }
        }
        return na;
    }

    public static NumberArrayResp convert(char[] cs) {
        NumberArrayResp na = new NumberArrayResp();
        if (cs != null) {
            na.value = new int[cs.length];
            for (int i = 0; i < cs.length; i++) {
                na.value[i] = cs[i];
            }
        }
        return na;
    }

    public static NumberArrayResp convert(short[] ss) {
        NumberArrayResp na = new NumberArrayResp();
        if (ss != null) {
            na.value = new int[ss.length];
            for (int i = 0; i < ss.length; i++) {
                na.value[i] = ss[i];
            }
        }
        return na;
    }

    public static NumberArrayResp convert(int[] is) {
        NumberArrayResp na = new NumberArrayResp();
        na.value = is;
        return na;
    }
}
