package com.estatetrader.dubboext;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.Map;

/**
 * dubbo的默认实现会自动将来自上游的旁路通信（attachment）传递给下游
 * 这种默认设定会导致下游的服务提供方收到其意料之外的信息，从而增加了服务间不相关逻辑相互干扰的可能性
 *
 * 我们讨论一种常见的场景：
 * 1. client调用服务A
 * 2. 服务A在处理client请求时调用了服务B
 *
 * 其中client称之为上游服务，服务A称之为当前服务，服务B称之为下游服务
 *
 * 在dubbo的实现中，ContextFilter（在当前服务被调用时执行）会将来自其上游的attachment保存到当前线程的RpcContext中，
 * 而AbstractClusterInvoker（在此服务调用其他服务时执行，即调用下游服务）会将当前线程的RpcContext中之前保存的attachment
 * 设置到invocation中，从而将其传递给其下游服务。
 *
 * 这种实现方式目前有两个问题：
 * 1. 目前的实现细导致本服务传递给下游服务的attachment被上游发来的attachment覆盖，这实质上导致了远端覆盖近端的不合理设定
 * 2. 当前服务在调用下游服务时，应完全控制其调用的参数（包括显式的业务参数和隐式的旁路信息）
 *    自动将上游的attachment传递给下游可能会影响下游的执行，导致不可预料的结果
 *
 * 基于以上讨论，我们引入了CallInfoProvider/CallInfoConsumerFilter和AttachmentClearProviderFilter来解决这个问题
 *
 * 1. 使用CallInfoProvider（在当前服务被调用时执行）将上游的attachment保存到DubboExtProperty
 * 2. 使用AttachmentClearProviderFilter（在当前服务被调用时执行）将RpcContext中由ContextFilter保存的attachment清理掉，
 *    从而防止这些attachment被AbstractClusterInvoker发送给下游服务
 * 3. 使用CallInfoConsumerFilter（在当前服务调用下游服务时执行）将非业务attachment传递给下游，
 *    包括uid/did/cid，这些attachment主要用于日志收集和调用链跟踪，不应影响业务执行
 *    注：某些业务方框架（如tcc）依赖于DubboExtProperty在上下游dubbo服务间传递链路信息，为了支持这些框架，DubboExtProperty中保存的所有callInfo都会传递给下游（不覆盖）
 *
 * 上述方案会导致服务实现方无法通过RpcContext获取上游的attachment，如果需要这个信息，请使用DubboExtProperty.getCallInfo()
 */
@Activate(group = CommonConstants.PROVIDER)
public class CallInfoProviderFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 如果某次处理请求的过程没有成功执行从而导致call info在这次请求开始的时候没有清理，则它会干扰本此请求
        // 因此需要显式删除
        // 此时间点是此次请求在当前服务开始被处理的时间起点，所以我们在此将call info记录下来
        DubboExtProperty.clearCallInfos();
        Map<String, String> attachments = invocation.getAttachments();

        if (attachments != null) {
            // 在开始服务的时候，我们将来自客户端的call info保存至线程变量中
            // 如果后续需要调用其他服务，则将这些信息传递给被调服务，从而形成调用信息的传递
            DubboExtProperty.putCallInfo(attachments);
        }

        // provider在异步模式下调用其他服务无法自动将调用者信息传递给被调服务，需显式跨线程传递
        Result result = invoker.invoke(invocation);
        // 无论是异步还是同步，我们都需要将这些信息删除，以免污染在此线程运行的其他服务
        DubboExtProperty.clearCallInfos();
        return result;
    }
}
