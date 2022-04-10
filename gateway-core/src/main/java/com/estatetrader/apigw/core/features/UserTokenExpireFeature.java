package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.core.GatewayException;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.rule.expire.ExpireReason;
import com.estatetrader.rule.expire.ExpireReasonType;
import com.estatetrader.util.Lambda;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.phases.executing.request.RequestFinished;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.responseEntity.Response;

import java.util.List;

/**
 * utk过期功能：
 * 1. 主动将已过期的utk降级为dtk
 * 2. 如果当前请求的确需要utk，则报告token过期错误码（或其他指定错误码）
 */
public interface UserTokenExpireFeature {
    /**
     * 主动将utk降级为dtk
     *
     * 这种降级策略表达了以下网关设计思路：
     * 1. dtk永不过期
     * 2. 过期的ntk/ptk/utk仍然具有dtk的功能
     * 3. 网关尽可能避免拒绝客户端请求
     *
     * 所有尝试renew utk的代码应早于此处理器
     */
    @Extension
    class AsyncTokenProcessorImpl implements SecurityFeature.AsyncTokenProcessor {
        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) {
            if (context.caller != null &&
                context.caller.uid > 0 &&
                context.caller.expire < context.startTime) {

                // utk已过期，提醒客户端清理并重新申请
                context.userTokenExpired = true;
                // utk降级为dtk
                context.caller.uid = 0;
                // dtk没有角色概念
                context.caller.role = null;
                // 保留ptk.partnerId
            }
        }
    }

    // @After(RequestSignatureFeature.class) // 因为utk过期并不影响请求签名验证，所以应优先报告签名错误
    @Extension(before = SecurityFeature.class) // token过期错误应早于其他安全检查错误
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {
        @Override
        public void verify(ApiContext context) throws GatewayException {
            if (!context.userTokenExpired ||
                // 只有在token已经过期，并且有特殊的过期原因时才报告错误
                // 普通的token过期错误应交由 SecurityFeature 处理
                context.userTokenExpireReason == null) {
                return;
            }

            ExpireReason reason = context.userTokenExpireReason;

            int server, client;
            // EXPIRED
            if (context.userTokenExpireReason.type == ExpireReasonType.SINGLE_DEVICE) {
                client = server = ApiReturnCode._C_SINGLE_DEVICE_ERROR;
            } else {
                server = ApiReturnCode._C_TOKEN_IN_USER_TOKEN_EXPIRED_LIST;
                client = ApiReturnCode._C_USER_TOKEN_ERROR;
            }

            ApiReturnCode display = new ApiReturnCode(Lambda.cascade(reason.message, "token已过期"), client);
            throw new GatewayException(new ApiReturnCode(server, display));
        }
    }

    @Extension
    class ResponseGeneratorImpl implements RequestFinished.ResponseGenerator {
        @Override
        public void process(ApiContext context, AbstractReturnCode code, List<ApiMethodCall> calls, Response response) {
            if (context.userTokenExpired) {
                response.needRenewUserToken = true;
            }
        }
    }
}
