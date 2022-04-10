package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.ArrayType;
import com.estatetrader.generic.CollectionLikeType;
import com.estatetrader.generic.CollectionType;
import com.estatetrader.generic.GenericArraySpliterator;

import java.util.Collection;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ElementReader {
    Stream<Object> stream(Object object);

    static ElementReader forType(CollectionLikeType type) {
        if (type instanceof ArrayType) {
            return array -> array == null ? Stream.empty()
                : StreamSupport.stream(new GenericArraySpliterator(array,
                Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
        } else if (type instanceof CollectionType) {
            return collection -> collection == null ? Stream.empty()
                : ((Collection<?>) collection).stream().map(x -> x);
        } else {
            throw new IllegalArgumentException("the specified type " + type + " is not an array or collection");
        }
    }
}
