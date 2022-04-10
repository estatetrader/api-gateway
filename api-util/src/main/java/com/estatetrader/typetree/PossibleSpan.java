package com.estatetrader.typetree;

import com.estatetrader.generic.StaticType;
import com.estatetrader.generic.UnionType;

import java.util.Objects;

public class PossibleSpan implements TypeSpan {

    private final UnionType unionType;
    private final StaticType possibleType;
    private final TypeSpanMetadata metadata;

    public PossibleSpan(UnionType unionType, StaticType possibleType, TypeSpanMetadata metadata) {
        this.unionType = unionType;
        this.possibleType = possibleType;
        this.metadata = metadata;
    }

    public UnionType getUnionType() {
        return unionType;
    }

    @Override
    public StaticType getEndType() {
        return possibleType;
    }

    @Override
    public TypeSpanMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PossibleSpan)) return false;

        PossibleSpan span = (PossibleSpan) object;

        if (!unionType.equals(span.unionType)) return false;
        if (!possibleType.equals(span.possibleType)) return false;
        if (!Objects.equals(metadata, span.metadata)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = unionType.hashCode();
        result = 31 * result + possibleType.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return possibleType.toString();
    }
}
