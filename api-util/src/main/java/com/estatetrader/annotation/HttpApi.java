package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.estatetrader.define.ApiOpenState;
import com.estatetrader.define.SecurityType;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpApi {
    /**
     * Http 接口名
     */
    String name();

    /**
     * Http 接口注释
     */
    String desc();

    /**
     * Http 接口短描述
     */
    String detail() default "";

    /**
     * 调用接口所需的安全级别
     */
    SecurityType security();

    /**
     * 接口开放状态
     */
    ApiOpenState state() default ApiOpenState.OPEN;

    /**
     * 接口负责人
     */
    String owner() default "";

    /**
     * Integrated级别接口，需要指定允许访问的第三方编号
     * 第三方集成接口需要明确的指定可以访问该资源的合作方
     *
     * @Deprecated 使用diamond进行配置，不由业务方指定
     */
    @Deprecated int[] allowThirdPartyIds() default {};

    /**
     * SecurityType.Integrated
     * 级别接口是否需要apigw进行签名验证,false:验证由服务提供方完成,true:apigw负责签名验证
     */
    boolean needVerify() default true;

    /**
     * 指示网关是否需要验证此API的验证码
     * @return 如果不需要网关验证其验证码，请设置为false
     */
    boolean needVerifyCode() default true;

    /**
     * 指示网关是否在API执行成功后在access日志中记录此API的返回值
     * 请在API的返回值重要程度很高并且返回值长度小于2m bytes时开启此选项
     * 注意：返回值长度如果超过2m bytes会被截断
     *
     * @return 如果需要记录返回值，则设置为true
     */
    boolean recordResult() default false;

    /**
     * <h3>API返回值的样例值，用于提升文档的可读性</h3>
     * <p>如果API返回值类型为实体类型，则忽略此字段，转而在实体的各个字段上使用 @ExampleValue 声明该API返回值的各字段样例值。</p>
     * <br>
     * <p>值的格式应与所修饰字段的类型的JSON序列化后格式一致。即所提供值应能够被反序列化为API返回值类型</p>
     * @return API返回值的样例值
     */
    String exampleValue() default "";
}
