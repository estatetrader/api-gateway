package com.estatetrader.dubboext;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

@Activate(group = CommonConstants.PROVIDER, order = Integer.MAX_VALUE)
public class AttachmentClearProviderFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 为防止dubbo自动向下游透传上游的attachment，这里将其全部清理掉
        // 业务方应通过DubboExtProperty获取这些attachment
        // 框架集成方应通过invocation直接获取attachment
        RpcContext.getContext().clearAttachments();
        return invoker.invoke(invocation);
    }
}
