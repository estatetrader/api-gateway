package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by steven on 20/06/2017.
 *
 * @deprecated 已废弃，须将被此注解修饰的HttpApi的权限级别调节至SecurityType.AuthorizedUser或SecurityType.User，取决于
 * 你是否需要对此API进行角色授权校验。
 * 如果你需要授权校验，即仅允许某些已授权的角色的用户访问，则应使用SecurityType.AuthorizedUser，否则使用SecurityType.User，
 * 即仅允许已经登录的用户访问。详细请参考网关身份认证与权限校验相关文档
 */
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSubSystem {
    /**
     * 子系统的名称
     */
    String value();

    /**
     * 指示是否验证当前用户是否有访问此API的权限
     */
    boolean authorizing() default true;
}
