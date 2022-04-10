package com.estatetrader.apigw.core.extensions;

@FunctionalInterface
public
interface ChainMethod<T, R, E extends Throwable> {
    R apply(T extension, Next<R, E> next) throws E;

    @FunctionalInterface
    interface NoResult<T, E extends Throwable> {
        void apply(T extension, Next.NoResult<E> next) throws E;
    }
}
