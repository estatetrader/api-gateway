package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记此API是否有签发etk的能力
 * 只有标注了此注解的API才能够签发etk
 *
 * 签发器本质上是一个拥有特定行为的API
 * 每个子系统的一个特定平台（aid）最多可以定义一个自己的签发器
 * 多个子系统之间不可以共享同一个签发器
 * 同一个子系统下的不同平台可以共享同一个签发器
 * 每个子系统的客户端仅应调用它所属子系统的etk签发器
 *
 * 后端API作者可以使用网关提供的如下两个注解注入请求中etk包含的字段：
 * 1. ExtensionAutowired 注入etk中的某一个指定字段
 * 2. ExtensionParamsAutowired 注入etk中的所有字段（map）
 *
 * 网关会校验所有etk注入字段的合法性，即这些字段必须出自于某个@ExtensionTokenIssuer
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionTokenIssuer {
    /**
     * 签发者从属的应用编号（aid）列表，这些aid仅应同属于同一个子系统
     * etk设计要求每个子系统的一个特定平台（aid）最多仅能定义一个签发器
     * 网关会阻止不在该名单中的平台调用此API
     *
     * @return 应用编号列表
     */
    int[] appIds();

    /**
     * 此签发器能够签发的所有etk的字段的集合
     * etk本质上是一组键值对，其中键称之为etk的字段名，而值称之为字段值
     * 每个子系统均可以定义一个etk签发器，并且可以自由选择它签发的etk包含哪些字段
     * 为了更好的规范化，签发器需要使用注解的fields表述它签发的etk最多能够包含哪些字段
     * 网关会对其实际签发的tk作校验，以确保它仅能签发它声明的字段
     * @return 签发的etk能够包含的字段集合
     */
    String[] fields();

    /**
     * 用于标识此签发器所签发的etk字段列表中哪个字段属于主要字段，
     * 这种字段可以用于取代之前utk/dtk中的subsystemMainId的注入
     * @return 设置主要字段的值
     */
    String mainField() default "";
}
