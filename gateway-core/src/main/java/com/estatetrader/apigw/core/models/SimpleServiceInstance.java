package com.estatetrader.apigw.core.models;

import com.estatetrader.apigw.core.contracts.ServiceInstance;

public class SimpleServiceInstance implements ServiceInstance {

    private final Object instance;

    public SimpleServiceInstance(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object get() {
        return instance;
    }
}
