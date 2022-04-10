package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseInjectProvider {
    /**
     * inject-provider名称，InjectFromApi需要和这个名称匹配
     */
    String value();
}
