package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义一个通用参数
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefineCommonParameter {
    /**
     * 标示此通用参数可以由前端指定
     * @return 为true表示可由前端指定
     */
    boolean fromClient();

    /**
     * 标示此通用参数可以被用于API自动注入的参数进行注入
     * @return 为true表示可声明为API自动注入的参数
     */
    boolean injectable();

    /**
     * 参数说明
     * @return 参数说明
     */
    String desc();
}
