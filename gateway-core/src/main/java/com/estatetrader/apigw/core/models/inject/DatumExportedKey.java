package com.estatetrader.apigw.core.models.inject;

import java.util.Objects;
import java.util.stream.Stream;

public class DatumExportedKey {
    private final String name;
    private final Object value;
    private final ElementReader elementReader;

    public DatumExportedKey(String name, Object value, ElementReader elementReader) {
        this.name = name;
        this.value = value;
        this.elementReader = elementReader;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public Stream<Object> getAsPlural() {
        return elementReader.stream(value).filter(Objects::nonNull);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DatumExportedKey)) return false;
        DatumExportedKey that = (DatumExportedKey) object;
        return name.equals(that.name) &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
