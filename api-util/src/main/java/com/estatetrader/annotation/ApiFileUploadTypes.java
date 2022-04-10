package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义多个
 * @see ApiFileUploadType
 * 为文件上传提供多种content-type选择
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiFileUploadTypes {
    ApiFileUploadType[] value();
}
