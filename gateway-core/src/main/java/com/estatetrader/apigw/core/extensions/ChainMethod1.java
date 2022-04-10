package com.estatetrader.apigw.core.extensions;

@FunctionalInterface
public
interface ChainMethod1<T, P1, R, E extends Throwable> {
    R apply(T extension, P1 param1, Next<R, E> next) throws E;

    @FunctionalInterface
    interface NoResult<T, P1, E extends Throwable> {
        void apply(T extension, P1 param1, Next.NoResult<E> next) throws E;
    }
}
