package com.estatetrader.apigw.core.models.inject;

import java.util.stream.Stream;

public class DatumProvidedValue {
    private final Object response;
    private final DatumReader reader;

    public DatumProvidedValue(Object response, DatumReader reader) {
        this.response = response;
        this.reader = reader;
    }

    public Stream<Object> stream() {
        return reader.stream(response);
    }
}
