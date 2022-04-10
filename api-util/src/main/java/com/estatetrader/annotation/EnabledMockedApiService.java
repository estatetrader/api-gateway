package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 放置于被@HttpApi修饰的API函数上，用于指示该API提供"已启用的被mock的API的列表服务"
 *
 * 本API由网关调用，用于确定当前哪些mock是有效的
 * API应返回一组API，网关根据这组API对当前请求的所有API进行过滤，并将包含在此API返回值中的API作为mock的目标对象
 * 并通过调用@ApiMockService API获取这个API的返回值（mock结果）
 *
 * \@EnabledMockedApisService 的功能和客户端提供的通用参数"_apis_to_mock"一致，后者优先级高于前者。
 *
 * \@EnabledMockedApisService 在客户端未传递通用参数_apis_to_mock时被网关调用
 *
 * 这种设计方式给了后端服务器组决定哪些API需要使用mock数据的能力
 *
 * 此API的返回值必须是一个字符串数组，包含所有已经启用mock的API列表，
 * 这个列表可以是全局固定的，也可以根据当前请求的上下文（用户、设备、平台等）信息进行计算，以实现例如不同平台、设备、用户拥有独立的mock
 * 选择权。
 *
 * 一般情况下，由于已经启用mock的API列表可能很长，而网关只需要跟当前请求有关的API列表（_mt参数），
 * 所以此API的实现方可以通过接收参数"requestApis"确定当前网关正在处理哪些API，并返回过滤之后的API
 *
 * 例如：
 *
 * \@EnabledMockedApiService
 * \@HttpApi
 * String filterEnabledMockedApis(
 *      \@ApiParameter(required = true, name = "requestApis", desc = "requestApis") String[] requestApis,
 *      \@ApiParameter(required = false, name = "deviceId", desc = "deviceId") int deviceId,
 *      \@ApiParameter(required = false, name = "appId", desc = "appId") int appId,
 * );
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnabledMockedApiService {
}
