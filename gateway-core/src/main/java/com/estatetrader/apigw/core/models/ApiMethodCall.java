package com.estatetrader.apigw.core.models;

import com.estatetrader.define.ApiCallInfo;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 封装一次接口调用信息的实体
 */
public class ApiMethodCall implements ApiCallInfo {

    private static final AtomicLong LAST_ID = new AtomicLong();

    /**
     * 执行该API call的节点名
     */
    public final String executionId;

    /**
     * 接口信息
     */
    public final ApiMethodInfo method;

    /**
     * 在api-method-call调用依赖网络中的直接前序节点，当前call必须在prev中的call执行之后执行
     */
    public LinkedList<ApiMethodCall> prev;

    /**
     * 在api-method-call调用依赖网络中的直接后续节点，当前call必须在next中的call执行之前执行
     */
    public LinkedList<ApiMethodCall> next;

    /**
     * 本次调用返回的隐式返回值
     */
    public Map<String, String> exportParams;

    /**
     * 客户端上传的业务id
     */
    public String businessId;

    /**
     * 调用结果(序列化前)
     */
    public Object result;

    /**
     * API的序列化结果
     */
    public ByteArrayOutputStream buffer;

    /**
     * 返回值长度(未压缩前的byte数组长度)
     */
    public int resultLen;

    /**
     * 序列化之后的返回值
     */
    public String serializedResult;

    /**
     * 执行中的额外消息
     */
    public final StringBuilder message = new StringBuilder();

    /**
     * 调用开始时间
     */
    public long startTime;

    /**
     * 调用耗时
     */
    public int costTime;

    /**
     * 错误码
     */
    private AbstractReturnCode code;

    /**
     * dubbo 服务返回需要api进行记录的日志信息
     */
    public String serviceLog;

    /**
     * 调用的具体参数
     */
    public String[] parameters;

    /**
     * 此次API调用是否直接由前端指定的（即包含在mt中）
     */
    public boolean fromClient;

    /**
     * 指示是否禁用返回值拦截器和返回值
     */
    public boolean disableResponseFilters;

    public ApiMethodCall(ApiMethodInfo method) {
        this.method = method;
        this.executionId = "$api-" + method.methodName + '@' + LAST_ID.incrementAndGet();
    }

    public void setCode(AbstractReturnCode code) {
        this.code = code;
    }

    public int getReturnCode() {
        return code != null ? code.getDisplay().getCode() : 0;
    }

    @Override
    public String getReturnMessage() {
        return code != null ? code.getDisplay().getDesc() : ApiReturnCode.SUCCESS.getDesc();
    }

    public int getOriginCode() {
        return code != null ? code.getCode() : 0;
    }

    public String getOriginMessage() {
        return code != null ? code.getDesc() : ApiReturnCode.SUCCESS.getDesc();
    }

    public void depend(ApiMethodCall call) {
        if(prev == null) {
            prev = new LinkedList<>();
        }
        prev.addLast(call);

        if (call.next == null) {
            call.next = new LinkedList<>();
        }
        call.next.addLast(this);
    }

    public boolean success() {
        return code == null || code.getCode() == ApiReturnCode._C_SUCCESS;
    }
}