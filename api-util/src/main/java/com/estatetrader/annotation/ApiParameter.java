package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.estatetrader.define.ApiParameterEncryptionMethod;
import com.estatetrader.define.EnumNull;
import com.estatetrader.define.ServiceInjectable;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiParameter {
    /**
     * 是否为必要参数
     */
    boolean required();

    /**
     * 为了防止在不安全网络环境下通信可能导致的敏感信息泄漏，某些敏感的API参数应以密文的方式传输
     * 按照参数的保密级别，我们提供以下加密方式，具体使用哪种加密方式完全由API定义决定
     *
     * 网关在收到客户端请求的参数后，按照API定义按照声明的加密方法对其进行解密，并使用解密后的明文调用对应的dubbo函数
     *
     * API提供方（即dubbo服务）无需对收到对参数进行解密处理
     *
     * @return 选择的参数加密方式
     */
    ApiParameterEncryptionMethod encryptionMethod() default ApiParameterEncryptionMethod.NONE;

    /**
     * 参数名称
     */
    String name();

    /**
     * 默认值
     */
    String defaultValue() default "";

    /**
     * 验证参数是否合法的
     */
    String verifyRegex() default "";

    /**
     * 参数验证失败的提示信息
     */
    String verifyMsg() default "";

    /**
     * 由于安全原因需要在日志系统中忽略的参数
     */
    boolean ignoreForSecurity() default false;

    /**
     * 不为默认值时表明该参数接受服务端注入
     * 注入的参数名即为serviceInject的值
     * 注入的参数值格式为半角逗号
     */
    Class<? extends ServiceInjectable> serviceInject() default ServiceInjectable.class;

    /**
     * 枚举类型定义, 用于描述当前字符串的取值范围而又不引入接口二进制兼容问题
     */
    Class<? extends Enum> enumDef() default EnumNull.class;

    /**
     * 该参数在接口中的次序, 与类型相关. 当前可能的取值有 int0, int1...int9 str0, str1...str9
     * 目前被用在etl处理接口调用日志时按照该顺序放置各个参数
     */
    String sequence() default "";

    /**
     * 参数注释
     */
    String desc();

    /**
     * <h3>参数的样例值，用于提升文档的可读性</h3>
     * <p>如果参数类型为实体类型，则忽略此字段，转而在实体的各个字段上使用 @ExampleValue 声明该参数的各字段样例值。</p>
     * <br>
     * <p>值的格式应与所修饰字段的类型的JSON序列化后格式一致。即所提供值应能够被反序列化为参数的声明类型</p>
     * @return 参数的样例值
     */
    String exampleValue() default "";
}
