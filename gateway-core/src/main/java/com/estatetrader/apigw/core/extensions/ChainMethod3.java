package com.estatetrader.apigw.core.extensions;

@FunctionalInterface
public
interface ChainMethod3<T, P1, P2, P3, R, E extends Throwable> {
    R apply(T extension, P1 param1, P2 param2, P3 param3, Next<R, E> next) throws E;

    @FunctionalInterface
    interface NoResult<T, P1, P2, P3, E extends Throwable> {
        void apply(T extension, P1 param1, P2 param2, P3 param3, Next.NoResult<E> next) throws E;
    }
}
