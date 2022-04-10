package com.estatetrader.apigw.core.models.inject;

import java.util.stream.Stream;

@FunctionalInterface
public interface DatumReader {
    Stream<Object> stream(Object response);
}
