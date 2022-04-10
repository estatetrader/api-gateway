package com.estatetrader.define;

import com.estatetrader.responseEntity.AuthenticationResult;

/**
 * 继承此接口为服务增加权限认证功能
 *
 * 场景：
 *  客服系统授权一名客服查看客人的订单列表。
 *  订单系统已经存在获取某个登录用户订单列表的API，网关会将登录用户的uid注入给订单系统的接口。
 *  但是由于安全原因客户端不能直接指定uid给订单系统，所以需要先调用一个授权接口，先确认当前登录用户是客服人员，然后再由网关将uid注入给订单系统。
 *
 * 解决方案：
 *  1. 客户端在一次http网关请求中同时请求客服系统的authenticate接口，以及订单接口（简称b），并且后者依赖前者。
 *  2. 根据依赖关系网关会先调用authenticate接口
 *  3. 客服系统验证当前用户是否为客服人员，然后返回可以注入的API列表和要注入的uid
 *  4. 网关收到AuthenticationResult，根据授权的API列表将uid注入给订单接口，然后调用订单系统
 *  5. 网关将两个接口的返回值包装返回给客户端，客户端显示订单列表
 */
public interface AuthenticationService {
    AuthenticationResult authenticate(long userid, String authType, String authInfo);
}