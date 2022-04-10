package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.define.Datum;

/**
 * 对需要注入的对象进行包装，使之兼容Datum接口，以绕开Java中对强类型字段赋值时的限制。
 * 在Json序列化时，需要将DatumWrapper剥离出去（客户端不应感知DatumWrapper类）
 */
public final class DatumWrapper implements Datum {
    private final Object value;

    public DatumWrapper(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
