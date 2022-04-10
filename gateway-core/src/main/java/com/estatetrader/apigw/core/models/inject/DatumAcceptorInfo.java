package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.define.Datum;
import com.estatetrader.generic.*;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatumAcceptorInfo {

    private static final Function<Object, Datum> WRAP_DATUM = obj
        -> obj == null || obj instanceof Datum ? (Datum) obj : new DatumWrapper(obj);

    private final GenericType type;
    private final Function<Stream<Object>, Object> converter;
    private final DatumWriter accessor;

    public DatumAcceptorInfo(GenericType type, DatumWriter accessor) {
        this.type = type;
        this.accessor = accessor;
        this.converter = createConverter(type);
    }

    public void write(Object container, Stream<Object> stream, String datumType) {
        accessor.write(container, converter.apply(stream), datumType);
    }

    public boolean isForField(GenericField field) {
        return accessor instanceof DatumWriter.ByField
            && ((DatumWriter.ByField) accessor).getField().getNativeField().equals(field.getNativeField());
    }

    private static Function<Stream<Object>, Object> createConverter(GenericType type) {
        StaticType datumClass = GenericTypes.of(Datum.class);
        // 单个datum注入
        if (type.isAssignableFrom(datumClass)) {
            return s -> {
                Iterator<Object> iter = s.iterator();
                if (iter.hasNext()) {
                    Object d = iter.next();
                    if (iter.hasNext()) {
                        throw new IllegalArgumentException("more than one element is found for datum import");
                    }
                    return WRAP_DATUM.apply(d);
                } else {
                    return null;
                }
            };
        }
        // 注入datum数组
        if (type instanceof ArrayType && ((ArrayType) type).getComponentType().isAssignableFrom(datumClass)) {
            return s -> s.map(WRAP_DATUM).toArray(Datum[]::new);
        }
        // 注入datum集合
        if (type instanceof CollectionType) {
            GenericType elementType = ((CollectionType) type).getElementType();
            if (elementType.isAssignableFrom(datumClass)) {
                return s -> s.map(WRAP_DATUM).collect(Collectors.toList());
            }
        }
        throw new IllegalArgumentException("unsupported datum type " + type);
    }
}
