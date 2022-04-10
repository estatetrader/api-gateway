package com.estatetrader.apigw.core.models;

import com.alibaba.fastjson.JSON;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.gateway.log.AccessLogEntry;
import com.estatetrader.gateway.log.RequestLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * api访问日志
 */
@Component
public final class GatewayLogger {
    private static final Logger       requestLogger = LoggerFactory.getLogger("request-logger");
    private static final Logger       accessLogger  = LoggerFactory.getLogger("access-logger");

    private static final int MAX_RESULT_LENGTH = 1024 * 1024; // 1m

    private boolean requestLogEnabled = true;

    @Value("${com.estatetrader.apigw.enableRequestLog:true}")
    public void setRequestLogEnabled(boolean requestLogEnabled) {
        this.requestLogEnabled = requestLogEnabled;
    }

    private boolean accessLogEnabled = true;

    @Value("${com.estatetrader.apigw.enableAccessLog:true}")
    public void setAccessLogEnabled(boolean accessLogEnabled) {
        this.accessLogEnabled = accessLogEnabled;
    }

    /**
     * no error
     */
    public void logRequest(ApiContext context, AbstractReturnCode returnCode) {
        if (!requestLogEnabled ||
            !requestLogger.isInfoEnabled()) {
            return;
        }

        RequestLogEntry entry = new RequestLogEntry();
        entry.call_id = context.cid;
        entry.app_id = String.valueOf(context.appId);
        entry.device_id = String.valueOf(context.deviceId);
        entry.user_id = String.valueOf(context.caller != null ? context.caller.uid : 0);
        entry.referer = context.referer;
        entry.client_ip = context.clientIP;
        entry.user_agent = context.agent;
        entry.request_url = context.getRequestString();
        entry.method = context.method;
        entry.token = context.token;
        if (context.newUserTokenResult != null) {
            entry.renewed_token = context.newUserTokenResult.newToken;
        }
        entry.extension_token = context.extensionToken;
        entry.access_time = String.valueOf(context.startTime);
        entry.cost = String.valueOf(context.costTime);
        entry.code = String.valueOf(returnCode.getCode());
        entry.return_code = String.valueOf(returnCode.getDisplay().getCode());
        entry.error_msg = returnCode.getDesc();
        entry.result_length = context.responseSize;
        entry.signature_type = context.signatureType;
        entry.third_party_bind_id = context.caller != null ? context.caller.partnerBindId : 0;
        if (!context.backendMessages.isEmpty()) {
            entry.consumed_bm_keys = context.backendMessages.stream()
                .map(m -> m.key)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
        }

        if (requestLogger.isInfoEnabled()) {
            requestLogger.info(JSON.toJSONString(entry));
        }
    }

    public void logAccess(ApiContext context, ApiMethodCall call) {
        if (!accessLogEnabled ||
            !accessLogger.isInfoEnabled()) {
            return;
        }

        AccessLogEntry entry = new AccessLogEntry();
        entry.call_id = context.cid;
        entry.app_id = String.valueOf(context.appId);
        entry.device_id = String.valueOf(context.deviceId);
        entry.user_id = String.valueOf(context.caller != null ? context.caller.uid : 0);
        entry.client_ip = context.clientIP;
        entry.referer = context.referer;
        entry.method = call.method.methodName;
        entry.user_agent = context.agent;
        entry.access_time = String.valueOf(call.startTime);
        entry.request_parameter = call.message.toString();
        entry.cost = String.valueOf(call.costTime);
        entry.real_code = String.valueOf(call.getOriginCode());
        entry.return_code = String.valueOf(call.getReturnCode());
        entry.utm_source = context.utm != null ? context.utm.get("source") : null;
        entry.response_log = call.serviceLog;
        entry.api_jar = call.method.jarFileSimpleName;

        entry.result_length = String.valueOf(call.resultLen);
        entry.result = truncate(call.serializedResult);

        entry.client_version = context.versionName;
        if (call.parameters != null) {
        Map<String, String> parameters = new LinkedHashMap<>(call.parameters.length);
            for (int i = 0; i < call.parameters.length; i++) {
                ApiParameterInfo pInfo = call.method.parameterInfos[i];
                String value = call.parameters[i] != null ? call.parameters[i] : pInfo.defaultValueInText;
                parameters.put(pInfo.name, truncate(value));
            }
            entry.parameters = JSON.toJSONString(parameters);
        }

        entry.third_party_bind_id = context.caller != null ? context.caller.partnerBindId : 0;

        if (accessLogger.isInfoEnabled()) {
            accessLogger.info(JSON.toJSONString(entry));
        }
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() > MAX_RESULT_LENGTH) {
            return text.substring(0, MAX_RESULT_LENGTH);
        }
        return text;
    }
}
