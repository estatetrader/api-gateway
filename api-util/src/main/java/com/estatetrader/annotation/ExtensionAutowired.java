package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注入etk params中的一个param
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionAutowired {
    /**
     * 要注入的etk中的字段名称，此名称必须由某个@ExtensionTokenIssuer定义
     * @return etk字段名
     */
    String value();

    /**
     * 如果此参数不存在，则拒绝本次API
     * @return 声明此字段是否为必填字段
     */
    boolean required() default false;
}