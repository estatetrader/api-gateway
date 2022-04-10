package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用此注解，将你的类型放置到固定的分组下。
 * 默认情况下，每个API在引用该类时都会将其放置在它所属的分组下，这样不同的API引用的同一个类型在文档生成时会有多个副本
 * 我们引入此字段解决这个问题
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityGroup {
    /**
     * 分组名称
     * @return 分组名称
     */
    String value();
}
