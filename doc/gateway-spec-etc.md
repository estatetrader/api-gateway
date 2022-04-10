# 3 其他网关功能

出API基础功能之外，网关还提供额外的一些功能。

## 3.1 OPEN API

## 3.2 文件上传

由dubbo服务在定义API的同时声明文件上传的信息（使用的云存储提供商、上传到哪个文件夹、支持的content-type和扩展名、针对每种扩展名的
文件大小限制）。

目前支持阿里云OSS、七牛云和腾讯点播系统（VOD）客户端使用`<input type=file name=folder.extension>`的方式使用文件上传功能，
其中folder是dubbo服务声明的文件夹，extension是要上传的文件扩展名（如果未定可以不用写）。网关根据客户端指定的folder去寻找dubbo
定义的文件上传信息，并确定目标云存储提供商，是否超过上传限制，并将文件上传到对应云到指定文件夹。

网关在上传文件之前会确认token是否合法，只有登录用户才能上传文件。网关会为上传的文件生成一个唯一文件名（如果是图片会包含尺寸），
并和路径一起组成完整的文件路径返回给客户端。客户端可以使用从网关返回的文件路径请求网关（get请求）获取文件内容。

使用`@ApiFileUploadInfo`进行文件上传的声明：

```java
/**
 * 声明一个文件上传的文件夹（上传到指定的云存储提供商的目录）
 *
 * 使用一个或多个
 * @see @ApiFileUploadType
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
```

## 3.3 DUBBO调用异常日志

网关通过DUBBO的插件机制，在每次DUBBO调用出现异常时，在provider端将错误日志统一打印出来。

## 3.4 DUBBO扩展

### 3.4.1 PageLink的Hessian反序列化兼容升级

dubbo服务之间如果需要hessian反序列化兼容，需要你们的消费方将api-util版本升级到3.1.15-SNAPSHOT。

所谓PageLink反序列化兼容，指的是将返回值字段从String修改为PageLink之后，在消费方（包括网关）获取到最新的api-jar之前这段时间内出现的hessian反序列化失败问题的兼容解决方案，api-util 3.1.15-SNAPSHOT会使用dubbo的SPI发现机制，在反序列化时，将不兼容的PageLink字段转换为合法的URL（字符串类型），从而解决反序列化的不兼容问题。

这种兼容方案能够允许我们平滑升级PageLink，但是由于技术问题，存在以下限制：

1. 仅限于实体字段类型从String转为PageLink类型的兼容升级（返回值本身从String转为PageLink不受支持，List<String>转为List<PageLink>也不受支持），包括API直接或者间接返回的实体以及List中使用的实体等
2. 除非PageLink本身指定了目标appId，默认情况只能转到H5（appId=1）的URL