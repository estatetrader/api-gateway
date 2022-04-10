package com.estatetrader.annotation;

import com.estatetrader.define.Bucket;
import com.estatetrader.define.FileUploadCloud;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个文件上传的文件夹（上传到指定的云存储提供商的目录）
 *
 * 使用一个或多个
 * @see ApiFileUploadType
 * 配置此上传目录可接受的content-type列表
 *
 * 网关会收集所有定义的上传文件夹并以此为依据处理客户端的文件上传请求
 * 客户端在上传文件时需要指定上传的文件的文件名和content-type
 *      文件名：格式为a.b（a不能包含小写字母，b为扩展名b不包含小数点）
 *      content-type：网关会搜索所有同时定义并支持该content-type的的@ApiFileUploadType，如果找不到则返回上传文件名错误异常
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiFileUploadInfo {

    /**
     * 是否私有读文件
     */
    Bucket bucket() default Bucket.PRIVATE;

    /**
     * 选择云存储的提供商，默认为阿里云OSS
     */
    FileUploadCloud cloud() default FileUploadCloud.OSS;

    /**
     * 该文件所在文件夹名（全大写字母）
     *  例如: 
     *      PRODUCT
     *      PRODUCT/IMG
     */
    String folderName();

    /**
     * 针对云存储的提供商提供额外参数
     */
    String options() default "";
}
