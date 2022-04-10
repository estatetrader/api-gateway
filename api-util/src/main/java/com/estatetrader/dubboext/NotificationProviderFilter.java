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

import java.util.HashMap;
import java.util.Map;

@Activate(group = CommonConstants.PROVIDER)
public class NotificationProviderFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(NotificationProviderFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        DubboExtProperty.clearNotifications();
        // 在当前线程创建notification map，这样我们可以确保此次请求中，当前线程所有获取到的notification map均为此map
        Map<String, String> notifications = DubboExtProperty.getCurrentNotifications();
        // 若provider中有异步逻辑，请在当前线程中获取notifications对象并传递到异步线程中
        Result result = invoker.invoke(invocation);
        if (logger.isDebugEnabled()) {
            logger.debug("notifications after invoke: {}", JSON.toJSONString(notifications));
        }

        // 在provider异步模式下，我们要清理掉此notification，以免干扰后续执行的其他过程
        DubboExtProperty.clearNotifications();

        return result.whenCompleteWithContext((appResponse, t) -> {
            // 构造一个notification map副本，这样我们可以安全地修改它
            Map<String, String> finalNotifications = new HashMap<>(notifications);
            // 在provider为异步的模式下，异步线程中新增的notification
            // 注意，这里指的是在回调线程中使用DubboExtProperty添加的notification
            DubboExtProperty.mergeNotifications(finalNotifications, DubboExtProperty.getCurrentNotifications());
            appResponse.addAttachments(finalNotifications);
            if (logger.isDebugEnabled()) {
                logger.debug("notifications after merge: {}", JSON.toJSONString(notifications));
            }
            DubboExtProperty.clearNotifications();
        });
    }
}