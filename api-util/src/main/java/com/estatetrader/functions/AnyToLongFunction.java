package com.estatetrader.functions;

@FunctionalInterface
public interface AnyToLongFunction<T> {
    long apply(T t);
}
