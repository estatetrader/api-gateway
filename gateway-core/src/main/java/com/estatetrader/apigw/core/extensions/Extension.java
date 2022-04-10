package com.estatetrader.apigw.core.extensions;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 插件编程模式的核心注解
 *
 * 用法如下：
 * 1. 定义能够描述插件格式的接口I，例如用于校验请求合法性的RequestVerifier
 * 2. 在特定路径上使用Extension\<I\>注入所有实现此插件的实例列表es
 * 3. 循环es中的每个元素，执行I中定义的函数，例如RequestVerifier.verify
 * 4. 将@Extension放置于实现接口I的实现类上
 *
 * 通过这种方式，可以实现类似于钩子的编程模式，将核心流程和非核心流程分离开
 *
 * 注意：插件框架会根据first()/last()/before()/after()对所有实现类进行排序
 */
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Extension {

    /**
     * spring bean的名称
     * @return bean名称
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

    /**
     * 将此实现类标记为第一个实现类（最多只能设置一个实现类）
     * 往往用在插件的主要实现类上
     * @return 是否为第一个实现类
     */
    boolean first() default false;

    /**
     * 将此实现类标记为最后一个实现类（最多只能设置一个实现类）
     * 往往用在插件的默认实现类上
     * @return 是否为最后一个实现类
     */
    boolean last() default false;

    /**
     * 表示此实现类应放置于指定的实现类之前（用于排序）
     * @return 其他实现类或者实现类所属的外层类（接口）
     */
    Class<?>[] before() default {};

    /**
     * 表示此实现类应放置于指定的实现类之后（用于排序）
     * @return 其他实现类或者实现类所属的外层类（接口）
     */
    Class<?>[] after() default {};

    /**
     * 表示此插件将替换指定的插件
     * @return 需要替换的插件列表
     */
    Class<?>[] replace() default {};

    /**
     * 是否将此插件声明为默认插件，如果有其他插件的存在，则此插件会自动禁用
     * @return 是否声明为默认插件
     */
    boolean defaults() default false;
}
