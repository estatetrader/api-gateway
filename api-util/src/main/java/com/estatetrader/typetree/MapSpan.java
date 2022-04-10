package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.MapType;

import java.util.Objects;

public class MapSpan implements TypeSpan {

    private final MapType mapType;
    private final TypeSpanMetadata metadata;

    public MapSpan(MapType mapType, TypeSpanMetadata metadata) {
        this.mapType = mapType;
        this.metadata = metadata;
    }

    public MapType getMapType() {
        return mapType;
    }

    @Override
    public GenericType getEndType() {
        return mapType.getValueType();
    }

    @Override
    public TypeSpanMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof MapSpan)) return false;

        MapSpan that = (MapSpan) object;

        if (!mapType.equals(that.mapType)) return false;
        if (!Objects.equals(metadata, that.metadata)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mapType.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return mapType.toString();
    }
}
