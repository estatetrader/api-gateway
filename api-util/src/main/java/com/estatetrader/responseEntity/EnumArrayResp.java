package com.estatetrader.responseEntity;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 这个是有必要的否则生成很多没有意义的EnumArrayResp
 */
@Description("字符串数组返回值")
@GlobalEntityGroup
public final class EnumArrayResp<E> implements Serializable {

    private static final long serialVersionUID = 1L;
    @Description("字符串数组返回值")
    public Collection<String> value;

    public static <E> EnumArrayResp<E> convert(Collection<E> ss) {
        EnumArrayResp<E> sa = new EnumArrayResp<>();
        sa.value = new ArrayList<>(ss.size());
        for (E e : ss) {
            sa.value.add(e != null ? e.toString() : null);
        }
        return sa;
    }

    public static <E> EnumArrayResp<E> convert(E[] ss) {
        EnumArrayResp<E> sa = new EnumArrayResp<>();
        sa.value = new ArrayList<>(ss.length);
        for (E e : ss) {
            sa.value.add(e != null ? e.toString() : null);
        }
        return sa;
    }
}
