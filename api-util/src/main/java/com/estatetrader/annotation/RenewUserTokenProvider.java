package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于指示被标记的API具备续签utk的功能
 *
 * 有且仅有一个API能够被标注这个注解
 *
 * 续签utk的工作方式：
 * 1. 网关在处理客户端请求时发现一个已经过期的token (expire字段）
 * 2. 网关校验此token是否可以续签（检查renewWindow字段），即：
 *      expire + renewWindow < now
 * 3. 如果可以续签，则网关调用renewToken方法，并将此token作为expiredToken测参数值传递给你
 * 4. 如果返回值非null，则继续处理http请求，并将返回值返回给客户端，指示客户端更新token
 * 5. 在其他情况下（你返回的时null，或者token已经过了续签时间），网关拒绝此次http请求，指示客户端重新登录
 *
 * 注意：请不要抛出任何异常，如果无法续签，则返回一个null值，返回任何非null值意味着续签成功，网关不会做进一步的校验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RenewUserTokenProvider {
}
