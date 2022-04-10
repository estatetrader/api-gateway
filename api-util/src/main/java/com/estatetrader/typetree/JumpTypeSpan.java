package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;

import java.util.Objects;

public class JumpTypeSpan implements TypeSpan {
    private final GenericType endType;
    private final TypeSpanMetadata metadata;

    public JumpTypeSpan(GenericType endType, TypeSpanMetadata metadata) {
        this.endType = endType;
        this.metadata = metadata;
    }

    @Override
    public GenericType getEndType() {
        return endType;
    }

    @Override
    public TypeSpanMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof JumpTypeSpan)) return false;

        JumpTypeSpan jumpTypeSpan = (JumpTypeSpan) object;

        if (!endType.equals(jumpTypeSpan.endType)) return false;
        if (!Objects.equals(metadata, jumpTypeSpan.metadata)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = endType.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return endType.toString();
    }
}
