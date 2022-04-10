package com.estatetrader.apigw.core.models;

import com.estatetrader.core.GatewayException;
import com.estatetrader.define.ApiParameterAccessor;
import com.estatetrader.define.CommonParameter;

public class ApiParameterAccessorImpl implements ApiParameterAccessor {

    private final ApiContext context;
    private final ApiMethodCall call;

    public ApiParameterAccessorImpl(ApiContext context, ApiMethodCall call) {
        this.context = context;
        this.call = call;
    }

    @Override
    public Object getParameter(String name) throws GatewayException {
        ApiParameterInfo info = null;
        String text = null;
        for (int i = 0; i < call.parameters.length; i++) {
            ApiParameterInfo p = call.method.parameterInfos[i];
            if (p.name.equals(name)) {
                info = p;
                text = call.parameters[i];
                break;
            }
        }
        if (info == null) {
            throw new IllegalArgumentException("could not find parameter " + name);
        }

        return info.convert(text);
    }

    @Override
    public String getCommonParameter(String name) {
        switch (name) {
            case CommonParameter.userAgent:
                return context.agent;
            case CommonParameter.applicationId:
                return String.valueOf(context.appId);
            case CommonParameter.clientIp:
                return context.clientIP;
            case CommonParameter.versionCode:
                return context.versionCode;
            case CommonParameter.versionName:
                return context.versionName;
            case CommonParameter.host:
                return context.host;
            case CommonParameter.callId:
                return context.cid;
            case CommonParameter.referer:
                return context.referer;
            default:
                throw new IllegalArgumentException(name);
        }
    }
}
