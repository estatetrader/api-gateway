package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标注一个API为mock service
 *
 * mock service是一种特殊的API，它能够为其他的API提供mock结果，以方便前端在后端未提供API实现时能访问到该API的mock数据
 * 需要将此注解放置于已被@HttpApi注解的方法上
 *
 * 该方法一般包含两个参数：
 * 1. mockedApi
 *      获取正在被mock的API的名称
 *      字符串类型
 * 2. apiParameters
 *      获取正在被mock的API的在此次请求中使用的所有业务参数
 *      字符串类型，JSON格式（键：参数名，值：参数的反序列化之后的值）
 *      网关会把被mock的API的所有参数放到一个map中，并对其进行json序列化
 *
 * 方法可以包含其他通用参数 （仅限于自动注入的参数）
 *
 * 方法的返回值必须为字符串类型，表示被mock api的mock结果,
 *
 * 例如：
 *
 * \@ApiMockService
 * \@HttpApi
 * String fetchMockData(
 *      \@ApiParameter(required = true, name = "mockedApi", desc = "requestApis") String mockedApi,
 *      \@ApiParameter(required = true, name = "apiParameters", desc = "apiParameters") String apiParameters
 * );
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMockService {
}
