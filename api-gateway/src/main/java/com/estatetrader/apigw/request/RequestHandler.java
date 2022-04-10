package com.estatetrader.apigw.request;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHandler {
    String handlerName();
    String[] urlPatterns() default {};
    String[] methods() default {"GET", "POST"};
}
