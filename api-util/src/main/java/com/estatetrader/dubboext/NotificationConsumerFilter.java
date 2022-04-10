package com.estatetrader.dubboext;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 收集在当前线程中调用的所有dubbo时得到的所有notification，并将其合并到当前线程的notifications中
 * 注意：仅支持在provider处理线程中同步或异步调用其他dubbo服务
 */
@Activate(group = CommonConstants.CONSUMER)
public class NotificationConsumerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumerFilter.class);

    /**
     * 是否关闭此过滤器
     */
    public static boolean disabled;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 选择性关闭此filter
        if (disabled) {
            return invoker.invoke(invocation);
        }

        // 我们不仅收集dubbo服务在处理一次服务中自身产生的notification
        // 也收集它在此次请求中调用其他服务时，被调用的服务产生的notification
        // 这些notification会进行合并，然后统一返回给此次请求的最终发起方（例如网关）
        // 获取当前线程的notifications引用
        Map<String, String> notifications = DubboExtProperty.getCurrentNotifications();
        if (logger.isDebugEnabled()) {
            logger.debug("notifications before invoke: {}", JSON.toJSONString(notifications));
        }

        return invoker.invoke(invocation).whenCompleteWithContext(((appResponse, throwable) -> {
            // 我们在新的回调线程中将notifications合并到之前线程的notifications中
            DubboExtProperty.mergeNotifications(notifications, appResponse.getAttachments());
            if (logger.isDebugEnabled()) {
                logger.debug("notifications after invoke: {}", JSON.toJSONString(notifications));
            }
        }));
    }
}