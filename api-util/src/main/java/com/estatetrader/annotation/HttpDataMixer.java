package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpDataMixer {
    /**
     * 数据混合器名称
     */
    String name();

    /**
     * 数据混合器描述
     */
    String desc();

    /**
     * 接口负责人
     */
    String owner();

    /**
     * 使用这个数据混合器的页面路径, 例如  /orderlist.html
     */
    String pagePath();

    /**
     * 可选的API列表
     */
    String[] optional() default {};
}
