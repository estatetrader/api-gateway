package com.estatetrader.dubboext;

import com.estatetrader.define.CommonParameter;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

@Activate(group = CommonConstants.PROVIDER, order = -1)
public class ApiInvokeLoggerFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiInvokeLoggerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 按照设计，主调方应主动设置cid，但是如果没有设置，我们会自动生成一个
        if (invocation.getAttachment(CommonParameter.callId) == null) {
            String cid = UUID.randomUUID().toString();
            invocation.setAttachment(CommonParameter.callId, cid);
        }

        put(CommonParameter.callId, invocation.getAttachment(CommonParameter.callId));
        put(CommonParameter.deviceId, invocation.getAttachment(CommonParameter.deviceId));
        put(CommonParameter.userId, invocation.getAttachment(CommonParameter.userId));

        Result res = invoker.invoke(invocation);

        MDC.remove(CommonParameter.userId);
        MDC.remove(CommonParameter.deviceId);
        MDC.remove(CommonParameter.callId);

        return res;
    }

    private void put(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
