package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 放置于泛型参数的wildcard之上，用于限定该wildcard允许的所有类型，或者接口/抽象类型之上，用于限定该类型的所有可能取值
 */
@Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.TYPE, ElementType.TYPE_PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedTypes {
    Class<?>[] value();
}
