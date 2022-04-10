package com.estatetrader.apigw.core.models;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.define.ApiCallCallback;
import com.estatetrader.define.MockApiConfigInfo;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.entity.ExtensionCallerInfo;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;

import com.estatetrader.gateway.backendmsg.PolledBackendMessage;
import com.estatetrader.responseEntity.RenewTokenResult;
import com.estatetrader.rule.expire.ExpireReason;

import java.util.*;

/**
 * Api请求上下文信息，网关在处理每个客户端请求时，会创建一个ApiContext来承载此次处理的状态
 */
public class ApiContext {

    /**
     * async context
     */
    public final GatewayRequest request;
    public final GatewayResponse response;

    public final ApiSchema apiSchema;

    public final WorkflowExecution[] executeApiCall;

    public ApiContext(GatewayRequest request, GatewayResponse response, ApiSchema apiSchema, WorkflowExecution[] executeApiCall) {
        this.request = request;
        this.response = response;
        this.apiSchema = apiSchema;
        this.executeApiCall = executeApiCall;
    }

    /**
     * 客户端指定要执行的API，按照API组合语法，可能包含多个API
     */
    public String method;

    /**
     * 接口调用列表
     */
    public List<ApiMethodCall> apiCalls = Collections.emptyList();

    /**
     * 是否为ssl链接
     */
    public boolean httpsMode;

    /**
     * 访问信息
     */
    public Map<String, String> requestInfo;

    public final void ignoreParameterForSecurity(String key) {
        if (requestInfo != null) {
            requestInfo.remove(key);
        }
    }

    public final String getRequestString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(httpsMode ? "https://" : "http://");
        sb.append(host);
        sb.append("/m.api?");
        if (requestInfo != null) {
            for (Map.Entry<String, String> entry : requestInfo.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null){
                    continue;
                }

                sb.append(key).append("=").append(value);
                sb.append("&");
            }
        }

        if (sb.charAt(sb.length() - 1) == '&') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * http请求的标识符
     */
    public String cid;

    /**
     * 设备序列号,业务用，来源于客户端通用参数_did，但是如果有合法的token，则使用token中的did
     */
    public long deviceId;

    /**
     * 客户端应用版本号
     */
    public String versionCode;

    /**
     * 客户端应用版本名 例:1.6.0
     */
    public String versionName;

    /**
     * 应用编号,显示传参的_aid
     */
    public int appId;

    /**
     * 访问时间
     */
    public long startTime = 0;

    /**
     * 时间开销
     */
    public int costTime;

    /**
     * 客户端信息
     */
    public String agent;

    /**
     * http referer
     */
    public String referer;

    /**
     * 访问站点
     */
    public String host;

    /**
     * 清除用戶cookie中的token信息
     */
    public boolean userTokenExpired;

    /**
     * 清除拓展用户cookie中的token信息
     */
    public boolean clearExtensionToken;

    /**
     * 清除operator cookie
     */
    public boolean clearOperatorToken;

    /**
     * 当前用户的名称（签发token时的用户名）
     */
    public String operator;

    /**
     * 最终客户端IP
     */
    public String clientIP;

    /**
     * Token
     */
    public String token;

    /**
     * extension token 用与存放拓展的授权信息
     */
    public String extensionToken;

    /**
     * device token
     */
    public String deviceToken;

    /**
     * Security Level 本次调用所需的综合安全级别
     */
    public int requiredSecurity;

    /**
     * 调用者信息
     */
    public CallerInfo caller;

    /**
     * 拓展的调用者信息
     */
    public ExtensionCallerInfo extCaller;

    /**
     * 设备调用者信息
     */
    public CallerInfo deviceCaller;

    /**
     * a renewed token fetched from renew token service
     */
    public RenewTokenResult newUserTokenResult;

    /**
     * 返回给客户端的后台消息
     */
    public final List<PolledBackendMessage> backendMessages = new ArrayList<>();

    /**
     * 客户端传上来的 cookie
     */
    private final Map<String, String> cookies = new HashMap<>();

    /**
     * 添加 cookie
     */
    public final void addCookie(String key, String value) {
        cookies.put(key, value);
    }

    /**
     * 获取 cookie 值
     */
    public final String getCookie(String key) {
        return cookies.get(key);
    }

    /**
     * 子系统通用参数
     */
    public Map<String, String> subsystemParams;

    /**
     * 指示本次请求中有哪些API需要使用mock数据
     * 指示本次请求中有哪些API需要录制数据
     */
    public MockApiConfigInfo mockApiConfigInfo;
    
    /**
     * 指示当前请求是否来源于内网环境（及客户端和当前服务器部署于同一个环境中），用于在环境内部通信中跳过特定校验从而提高效率
     */
    public boolean fromInternalEnvironment;

    /**
     * 指示当前请求是否来源于可信赖网络（办公网络或特定的IP白名单），用于实现仅将部分功能或者API只开放给可信赖网络的功能
     */
    public boolean fromTrustedNetwork;

    /**
     * 如果不为null，并且此次请求没有其他request级别的错误出现，则会将此错误码设置为此次请求的错误码
     */
    public AbstractReturnCode requestErrorCode;

    /**
     * 用于存储token过期的原因，由UserTokenExpireFeature使用
     */
    public ExpireReason userTokenExpireReason;

    /**
     * 表示此时是否已经发起或者完成renew token，用于避免多个功能重复renew token
     */
    public boolean userTokenRenewed;

    /**
     * 记录返回值的总长度（序列化之后的字节数）
     */
    public int responseSize;

    /**
     * 表示此次签名使用的签名类型：dynamic（动态盐签名）, static（静态盐签名）, none（没有使用签名）, public-key（使用公钥）
     */
    public String signatureType;

    /**
     * 指示在处理此次请求时是否遇到了加密参数无法解密的情况
     */
    public boolean parameterDecryptionFailure;

    public Map<String, String> utm;

    public GatewayRequest getRequest() {
        return request;
    }

    public GatewayResponse getResponse() {
        return response;
    }

    /**
     * execute an api method call (async)
     * @param pipeline workflow pipeline
     * @param methodInfo api method info to execute
     * @param callback the callback which will be called after api executed
     */
    public void startApiCall(WorkflowPipeline pipeline, ApiMethodInfo methodInfo, ApiCallCallback<Object> callback) {
        new ApiCallExecutorImpl(pipeline, this.executeApiCall).start(methodInfo, callback);
    }

    /**
     * execute an api method call (async)
     * @param pipeline workflow pipeline
     * @param methodInfo api method info to execute
     * @param args args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    public void startApiCall(WorkflowPipeline pipeline, ApiMethodInfo methodInfo, Object[] args, ApiCallCallback<Object> callback) {
        new ApiCallExecutorImpl(pipeline, this.executeApiCall).start(methodInfo, args, callback);
    }

    /**
     * execute an api method call (async)
     * @param pipeline workflow pipeline
     * @param methodInfo api method info to execute
     * @param args args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    public void startApiCall(WorkflowPipeline pipeline, ApiMethodInfo methodInfo, Object[] args, ApiCallCallback.Complex<Object> callback) {
        new ApiCallExecutorImpl(pipeline, this.executeApiCall).start(methodInfo, args, callback);
    }
}