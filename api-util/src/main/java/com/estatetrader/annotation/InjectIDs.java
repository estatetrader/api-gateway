package com.estatetrader.annotation;

import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.entity.IDListInjector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数接受服务端注入
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectIDs {
    String value() default IDListInjector.DEFAULT_NAME;
    Class<? extends ServiceInjectable> serviceInject() default IDListInjector.class;
}
