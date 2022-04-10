package com.estatetrader.apigw.core.extensions;

public interface Next<R, E extends Throwable> {
    R go() throws E;
    R go(R result) throws E;
    R previousResult();

    interface NoResult<E extends Throwable> {
        void go() throws E;
    }
}
