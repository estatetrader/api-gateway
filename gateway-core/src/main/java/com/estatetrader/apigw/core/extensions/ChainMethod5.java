package com.estatetrader.apigw.core.extensions;

@FunctionalInterface
public
interface ChainMethod5<T, P1, P2, P3, P4, P5, R, E extends Throwable> {
    R apply(T extension, P1 param1, P2 param2, P3 param3, P4 param4, P5 param5, Next<R, E> next) throws E;

    @FunctionalInterface
    interface NoResult<T, P1, P2, P3, P4, P5, E extends Throwable> {
        void apply(T extension, P1 param1, P2 param2, P3 param3, P4 param4, P5 param5, Next.NoResult<E> next) throws E;
    }
}
