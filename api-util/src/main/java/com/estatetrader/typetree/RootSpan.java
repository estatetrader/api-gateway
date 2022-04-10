package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;

import java.util.Objects;

public class RootSpan implements TypeSpan {

    private final GenericType type;
    private final TypeSpanMetadata metadata;

    public RootSpan(GenericType type, TypeSpanMetadata metadata) {
        this.type = type;
        this.metadata = metadata;
    }

    @Override
    public GenericType getEndType() {
        return type;
    }

    @Override
    public TypeSpanMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof RootSpan)) return false;

        RootSpan rootSpan = (RootSpan) object;

        if (!type.equals(rootSpan.type)) return false;
        if (!Objects.equals(metadata, rootSpan.metadata)) return false;

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
        return "=>" + type;
    }
}
