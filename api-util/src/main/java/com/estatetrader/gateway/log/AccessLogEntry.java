package com.estatetrader.gateway.log;

import com.alibaba.fastjson.JSON;
import com.estatetrader.generic.GenericTypes;
import com.estatetrader.core.ParameterConverter;
import com.estatetrader.core.ParameterConvertingException;

import java.lang.reflect.Type;

/**
 * 网关access日志的记录格式
 * access日志表示网关在处理一次客户端请求（request日志）时的每个步骤（例如调用一次dubbo API或mixer等）
 * 一条request日志包含若干条access日志，并通过call_id字段相互关联
 * 由于日志实际存储时使用的是下划线分割方式命名，所以在此类中我们没有使用驼峰命名法
 */
public class AccessLogEntry {
    public String call_id;
    public String app_id;
    public String device_id;
    public String user_id;
    public String client_ip;
    public String referer;
    public String method;
    public String user_agent;
    public String access_time;
    public String request_parameter;
    public String result_length;
    public String result;
    public String cost;
    public String real_code;
    public String return_code;
    public String utm_source;
    /**
     * 由服务端打印出来的额外的信息，用于服务向网关日志解析系统提供额外信息（可能是返回值的一部分，用于日志分析）
     */
    public String response_log;
    /**
     * API隶属的api-jar文件名（不包含路径）
     */
    public String api_jar;
    /**
     * 当前请求者的客户端版本号
     */
    public String client_version;
    /**
     * 请求此API时使用的参数列表，格式为：JSON(map[name]->value)
     */
    public String parameters;
    /**
     * 三方登录授权的bindId
     */
    public long third_party_bind_id;

    /**
     * 获取指定的参数值（强类型）
     * @param name 指定要获取的参数的名称
     * @param parameterType 参数值的实际类型
     * @return 返回强类型化的参数值
     * @throws ParameterConvertingException 如果类型转换失败（参数被截断、或指定了错误的类型等）则抛出此异常
     */
    public Object getParameter(String name, Type parameterType) throws ParameterConvertingException {
        if (parameters == null) {
            return null;
        }

        try {
            String text = JSON.parseObject(parameters).getString(name);
            if (text == null) {
                return null;
            }

            return ParameterConverter.getConverter(GenericTypes.of(parameterType)).convert(text);
        } catch (Exception e) {
            throw new ParameterConvertingException("convert parameter " + name + " to " + parameterType + " failed", e);
        }
    }
}
