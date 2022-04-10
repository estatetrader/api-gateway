package com.estatetrader.apigw.core.extensions;

@FunctionalInterface
public
interface ChainMethod2<T, P1, P2, R, E extends Throwable> {
    R apply(T extension, P1 param1, P2 param2, Next<R, E> next) throws E;

    @FunctionalInterface
    interface NoResult<T, P1, P2, E extends Throwable> {
        void apply(T extension, P1 param1, P2 param2, Next.NoResult<E> next) throws E;
    }
}
