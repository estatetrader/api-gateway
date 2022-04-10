package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注入extension token中的所有字段，所修饰参数必须为map类型
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionParamsAutowired {
    /**
     * 如果etk不存在（或不合法），按照请求源分以下两种情况处理：
     * 1. 由客户端直接发起的API调用，则拒绝本次请求，并报告（-362）etk错误
     * 2. 由返回值注入或mixer调用，则拒绝本次API，并报告（-140）参数错误
     * @return 声明etk是否必须
     */
    boolean required() default false;
}