package com.estatetrader.annotation;

import com.estatetrader.define.ServiceInjectable;

import java.lang.annotation.*;

/**
 * 用于描述该接口返回时输出给其他接口隐式注入的参数列表
 * @deprecated replaced by #{@link ExportParam}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ParamsExport.class)
@Deprecated
public @interface ParamExport {
    String name();

    Class<? extends ServiceInjectable.InjectionData> dataType();
}
