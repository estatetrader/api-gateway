package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.estatetrader.define.MockApiReturnObject;

/**
 * Created by rendong on 15/7/12.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiShortCircuit {
    Class<? extends MockApiReturnObject> value();
}
