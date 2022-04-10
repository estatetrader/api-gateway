package com.estatetrader.annotation;

import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.entity.SimpleInjector;

import java.lang.annotation.*;

/**
 * 用于描述该接口返回时输出给其他接口隐式注入的参数列表
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ExportParams.class)
public @interface ExportParam {
    String name();

    Class<? extends ServiceInjectable.InjectionData> dataType() default SimpleInjector.Data.class;
}
