package com.estatetrader.typetree;

import com.estatetrader.generic.CollectionType;
import com.estatetrader.generic.GenericType;

import java.util.Objects;

public class CollectionSpan implements TypeSpan {

    private final CollectionType type;
    private final TypeSpanMetadata metadata;

    public CollectionSpan(CollectionType type, TypeSpanMetadata metadata) {
        this.type = type;
        this.metadata = metadata;
    }

    @Override
    public GenericType getEndType() {
        return type.getElementType();
    }

    @Override
    public TypeSpanMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CollectionSpan)) return false;

        CollectionSpan span = (CollectionSpan) object;

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
