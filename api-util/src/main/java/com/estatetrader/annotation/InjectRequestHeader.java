package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入http请求中的header值
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRequestHeader {
    /**
     * 要注入的header的名称
     * @return 要注入的header的名称
     */
    String value();

    /**
     * 是否应该在找不到相应的请求头时报告参数错误
     * @return 是否是必须参数
     */
    boolean required();
}
