package com.estatetrader.annotation;

import java.lang.annotation.*;

/**
 * 配合
 *  @see ApiFileUploadInfo
 * 使用，为ApiFileUploadInfo提供可支持的content-type列表以及文件最大长度限制
 * 本Annotation可以重复声明，以指定多个文件类型
 * 业务流程
 *  1. 客户端上传文件时需要提供这个文件上传的目录以及content-type
 *  2. 网关根据客户端指定的目录找到对应的@ApiFileUploadInfo
 *  3. 网关根据客户端指定的content-type找到对应的@ApiFileUploadType
 *  4. 如果extension定义，则使用该扩展名最为最终文件名的扩展名
 *  4. 网关继续检查上传的文件是否超过了最大长度限制
 *  5. 如果网关没有找到对应的 @ApiFileUploadType，则直接拒绝客户端的文件上传请求
 *
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiFileUploadTypes.class)
public @interface ApiFileUploadType {

    /**
     * 客户端请求携带的content-type的值，支持通配符。
     * 例如：
     *  image/*
     *  * /* （斜杠前面空格是为了和java注释区分开，使用时不要包含空格）
     *  audio/mp*
     */
    String contentType();

    /**
     * 受支持的文件扩展名，默认值代表需要网关使用客户端提供的扩展名
     */
    String extension() default "";

    /**
     * 文件能够允许的最大长度
     */
    int maxSize() default 5 * 1024 * 1024;

    /**
     * 指示此类型是否为该extension的主类型
     * 由于同一个extension可能会有多种content-type，所以在根据extension寻找对应的content-type的时候会出现多种后选项
     * 这时mainType为true的那个type会胜出
     * 每个文件夹的每种extension最多可以定义一个mainType为true的FileUploadType
     * 在contentType包含通配符时不可指定mainType
     */
    boolean mainType() default false;

    /**
     * 指示网关在上传此类型文件时，是否在最终生成的文件名中包含图片的纬度（长✖宽）信息
     * 只能在文件为图片类型时才可以设置此标志
     */
    boolean includeImageDimension() default false;
}
