package com.estatetrader.dubboext;

import com.estatetrader.define.CommonParameter;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

@Activate(group = CommonConstants.CONSUMER)
public class CallInfoConsumerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CallInfoConsumerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // dubbo服务的提供者在开始处理一次请求时，会将它的call info保存到DubboExtProperty中
        // 如果在处理请求时需要调用其他的服务，则应将这些call info传递给被调用者

        Map<String, String> callInfo = DubboExtProperty.getCallInfo();
        for (Map.Entry<String, String> entry : callInfo.entrySet()) {
            String key = entry.getKey();
            if (invocation.getAttachment(key) != null) {
                // call info应为此次调用的默认attachment，所以不应覆盖已有信息
                continue;
            }

            // 某些业务方框架（如tcc）依赖于DubboExtProperty在上下游dubbo服务间传递链路信息，
            // 为了支持这些框架，DubboExtProperty中保存的所有callInfo都会传递给下游（不覆盖）
            invocation.setAttachment(entry.getKey(), entry.getValue());
        }

        // 按照设计，主调方应主动设置cid，但是如果没有设置，我们会自动生成一个
        if (invocation.getAttachment(CommonParameter.callId) == null) {
            String cid = UUID.randomUUID().toString();
            invocation.setAttachment(CommonParameter.callId, cid);
        }

        return invoker.invoke(invocation);
    }
}
