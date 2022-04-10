package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标注一个API为scribe service
 *
 * scribe service是一种特殊的API，它能够为其他的API录制请求和结果，以方便前端在后端未提供API实现时能访问到该API的mock数据
 * 需要将此注解放置于已被@HttpApi注解的方法上
 *
 *
 *根据指定参数录制api调用信息
 *目前支持网关和Node
 *  scribeApi 需要被录制的API的名称
 * apiParameters 需要录制的api请求参数(需要保证该参数是可被反序列化的json格式)
 * targetDeviceId 需要被录制拦截的客户端的did
 * targetUserId 需要被录制拦截的客户端的userId
 * apiResult 需要录制的api的结果（json格式的字符串）
 *
 *@ApiScribeService
 *@HttpApi(name = "mock.scribeMockInfo", desc = "根据指定参数录制api调用信息", security = SecurityType.Internal, owner = "lin")
 *boolean scribeMockInfo(
         *@ApiParameter(required = true, name = "scribeApi", desc = "需要被录制的API的名称") String scribeApi,
        *@ApiParameter(required = true, name = "apiParameters", desc = "需要被录制的API的业务参数值") String apiParameters,
        *@ApiParameter(required = true, name = "targetDeviceId", desc = "需要被录制拦截的客户端的did") long targetDeviceId,
        *@ApiParameter(required = false, name = "targetUserId", desc = "需要被录制拦截的客户端的userId") long targetUserId,
        *@ApiParameter(required = true, name = "apiResult", desc = "需要被录制的API的结果值") String apiResult
        *);
 *
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiScribeService {
}
