package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于描述该接口返回时输出给其他接口隐式注入的参数列表
 * @deprecated replaced by #{@link ExportParams}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface ParamsExport {
    ParamExport[] value();
}
