package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接收来自客户端的_ssp参数，并将其反序列化为map
 * 作为API的参数注入值
 * 参数必须为map类型
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubsystemParamsAutowired {
}