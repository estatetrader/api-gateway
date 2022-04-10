package com.estatetrader.annotation;

import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.entity.SimpleInjector;

import java.lang.annotation.*;

/**
 * 参数接受服务端注入
 * @deprecated replaced by #{@link ImportParam}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface InjectParam {
    String value();

    Class<? extends ServiceInjectable> serviceInject() default SimpleInjector.class;
}
