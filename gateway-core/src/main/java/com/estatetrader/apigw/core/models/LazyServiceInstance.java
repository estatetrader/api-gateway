package com.estatetrader.apigw.core.models;

import com.estatetrader.apigw.core.contracts.ServiceInstance;

import java.util.function.Supplier;

public class LazyServiceInstance implements ServiceInstance {
    private final Supplier<Object> supplier;
    private volatile Object instance;

    public LazyServiceInstance(Supplier<Object> supplier) {
        this.supplier = supplier;
    }

    @Override
    public Object get() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = supplier.get();
                }
            }
        }

        return instance;
    }
}
