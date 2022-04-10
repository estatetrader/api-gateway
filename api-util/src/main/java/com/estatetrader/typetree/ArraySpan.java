package com.estatetrader.typetree;

import com.estatetrader.generic.ArrayType;
import com.estatetrader.generic.GenericType;

import java.util.Objects;

public class ArraySpan implements TypeSpan {

    private final ArrayType type;
    private final TypeSpanMetadata metadata;

    public ArraySpan(ArrayType type, TypeSpanMetadata metadata) {
        this.type = type;
        this.metadata = metadata;
    }

    @Override
    public GenericType getEndType() {
        return type.getComponentType();
    }

    @Override
    public TypeSpanMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ArraySpan)) return false;

        ArraySpan span = (ArraySpan) object;

        if (!type.equals(span.type)) return false;
        if (!Objects.equals(metadata, span.metadata)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
