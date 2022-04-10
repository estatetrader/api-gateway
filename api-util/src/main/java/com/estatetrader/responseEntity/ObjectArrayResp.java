package com.estatetrader.responseEntity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.GlobalEntityGroup;

@Description("对象数组返回值")
@GlobalEntityGroup
public final class ObjectArrayResp<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Description("对象数组返回值")
    public Collection<T> value;

    public static <T> ObjectArrayResp<T> convert(T[] array) {
        ObjectArrayResp<T> arrayResp = new ObjectArrayResp<>();
        if (array != null) {
            arrayResp.value = Arrays.asList(array);
        }
        return arrayResp;
    }

    public static <T> ObjectArrayResp<T> convert(Collection<T> collection) {
        ObjectArrayResp<T> arrayResp = new ObjectArrayResp<>();
        arrayResp.value = collection;
        return arrayResp;
    }
}
