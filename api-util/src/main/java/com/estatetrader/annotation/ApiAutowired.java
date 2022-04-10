package com.estatetrader.annotation;

import com.estatetrader.define.ServiceInjectable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by rendong on 14-4-24.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAutowired {
    String value();

    /**
     * 不为默认值时表明该参数接受服务端注入
     * 注入的参数名即为serviceInject的值
     * 注入的参数值格式为半角逗号
     */
    Class<? extends ServiceInjectable> serviceInject() default ServiceInjectable.class;
}
