package com.estatetrader.dubboext;

import com.estatetrader.define.CommonParameter;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.entity.ServiceException;
import com.estatetrader.entity.ServiceRuntimeException;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 为dubbo provider统一提供异常捕获处理。由于该Filter会侵入异常处理流程，因此不强制使用。需服务开发者自行在META-INF中配置
 *
 * @author rendong
 */
@Activate(group = CommonConstants.PROVIDER, order = 1)
public class ExceptionHandleProviderFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandleProviderFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            return invoker.invoke(invocation).whenCompleteWithContext((r, e) -> {
                put(CommonParameter.callId, invocation.getAttachment(CommonParameter.callId));
                put(CommonParameter.deviceId, invocation.getAttachment(CommonParameter.deviceId));
                put(CommonParameter.userId, invocation.getAttachment(CommonParameter.userId));
                try {
                    if (r.hasException()) {
                        r.setException(wrapException(r.getException()));
                    }
                    if (e != null) {
                        r.setException(wrapException(e));
                    }
                } finally {
                    MDC.remove(CommonParameter.userId);
                    MDC.remove(CommonParameter.deviceId);
                    MDC.remove(CommonParameter.callId);
                }
            });
        } catch (Throwable t) {
            return AsyncRpcResult.newDefaultAsyncResult(wrapException(t), invocation);
        }
    }

    private ServiceException wrapException(Throwable t) {
        if (t instanceof ServiceRuntimeException) {
            ServiceRuntimeException sre = (ServiceRuntimeException)t;
            LOGGER.error("api service runtime exception.", t);
            return new ServiceException("error code catched.", sre);
        } else if (t instanceof ServiceException) {
            LOGGER.error("api service exception.", t);
            return (ServiceException) t;
        } else {
            LOGGER.error("api undesigned exception.", t);
            return new ServiceException(ApiReturnCode.DUBBO_SERVICE_ERROR, "api failed. msg:" + t.getMessage(), t);
        }
    }

    private void put(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
