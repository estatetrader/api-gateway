package com.estatetrader.apigw.core.support;

import com.estatetrader.define.CommonParameter;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiContext;
import org.slf4j.MDC;

public interface ApiMDCSupport {

    default void setupMDC(ApiContext context) {
        setupMDC(context, null);
    }

    default void setupMDC(ApiContext context, ApiMethodInfo method) {
        MDC.clear();

        MDC.put(CommonParameter.callId, context.cid);
        MDC.put(CommonParameter.clientIp, context.clientIP);
        MDC.put(CommonParameter.applicationId, String.valueOf(context.appId));
        MDC.put(CommonParameter.deviceId, String.valueOf(context.deviceId));
        if (method != null) {
            MDC.put(CommonParameter.method, method.methodName);
        }

        if (context.caller != null) {
            MDC.put(CommonParameter.userId, String.valueOf(context.caller.uid));
        }
        if (context.extCaller != null) {
            MDC.put(CommonParameter.extensionUserId, String.valueOf(context.extCaller.eid));
        }
    }

    default void cleanupMDC() {
        MDC.clear();
    }
}
