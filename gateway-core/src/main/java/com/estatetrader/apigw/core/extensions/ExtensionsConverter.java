package com.estatetrader.apigw.core.extensions;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ExtensionsConverter<T> implements Converter<Collection<T>, Extensions<T>> {
    @Override
    @Nullable
    public Extensions<T> convert(Collection<T> ts) {
        return new Extensions.ExtensionsImpl<>(ts);
    }
}
