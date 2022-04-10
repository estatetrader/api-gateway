package com.estatetrader.annotation;

import com.estatetrader.define.ResponseFilter;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FilterResponseRepeated.class)
public @interface FilterResponse {
    Class<? extends ResponseFilter> type();
}