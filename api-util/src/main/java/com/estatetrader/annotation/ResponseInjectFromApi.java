package com.estatetrader.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ResponseInjectFromApiRepeated.class)
public @interface ResponseInjectFromApi {
    String value();
}