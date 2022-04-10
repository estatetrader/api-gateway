package com.estatetrader.typetree;

import com.estatetrader.generic.GenericField;
import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.StaticType;

import java.util.Objects;

public class FieldSpan implements TypeSpan {

    private final StaticType ownerType;
    private final GenericField field;
    private final TypeSpanMetadata metadata;

    public FieldSpan(StaticType ownerType, GenericField field, TypeSpanMetadata metadata) {
        this.ownerType = ownerType;
        this.field = field;
        this.metadata = metadata;
    }

    public StaticType getOwnerType() {
        return ownerType;
    }

    public GenericField getField() {
        return field;
    }

    @Override
    public GenericType getEndType() {
        return field.getResolvedType();
    }

    @Override
    public TypeSpanMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof FieldSpan)) return false;

        FieldSpan fieldSpan = (FieldSpan) object;

        if (!ownerType.equals(fieldSpan.ownerType)) return false;
        if (!field.equals(fieldSpan.field)) return false;
        if (!Objects.equals(metadata, fieldSpan.metadata)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerType.hashCode();
        result = 31 * result + field.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ownerType + "." + field.getName() + "(" + field.getResolvedType() + ")";
    }
}
