package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.core.GatewayException;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.rule.cases.RequestVerifyCodeService;
import com.estatetrader.util.Lambda;
import com.estatetrader.apigw.core.models.ApiContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 验证码功能
 */
public interface VerifyCodeFeature {

    @Component
    class Config {
        @SuppressWarnings("FieldMayBeFinal")
        @Value("${com.estatetrader.apigw.enableRequestVerifyCode:true}")
        private boolean featureEnabled = true;
    }

    // @After(RequestSignatureFeature.class) // 签名错误优先
    @Extension(before = SecurityFeature.class)
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {

        private final RequestVerifyCodeService.Listeners requestVerifyCodeListeners;
        private final Config config;

        public RequestVerifierImpl(@Autowired(required = false)
                                       RequestVerifyCodeService.Listeners requestVerifyCodeListeners,
                                   Config config) {
            this.requestVerifyCodeListeners = requestVerifyCodeListeners;
            this.config = config;
        }

        @Override
        public void verify(ApiContext context) throws GatewayException {
            if (!config.featureEnabled ||
                requestVerifyCodeListeners == null) {
                return;
            }

            // white list

            if (context.apiCalls == null || Lambda.all(context.apiCalls, c -> !c.method.needVerifyCode)) {
                // all apis currently called do not need verify code
                return;
            }

            if (context.caller == null) {
                if (requestVerifyCodeListeners.excludeByDevice.containsKey(context.deviceId)) {
                    return;
                }
            } else {
                if (requestVerifyCodeListeners.excludeByDevice.containsKey(context.caller.deviceId)) {
                    return;
                }
                if (requestVerifyCodeListeners.excludeByUser.containsKey(context.caller.uid)) {
                    return;
                }
            }

            // black list

            if (context.clientIP != null &&
                requestVerifyCodeListeners.byIp.containsKey(context.clientIP)) {
                throw new GatewayException(ApiReturnCode.REQUIRE_VERIFY_CODE_BY_CLIEN_IP);
            }

            if (context.caller == null) {
                if (requestVerifyCodeListeners.byDevice.containsKey(context.deviceId)) {
                    throw new GatewayException(ApiReturnCode.REQUIRE_VERIFY_CODE_BY_DEVICE_ID);
                }
            } else {
                if (requestVerifyCodeListeners.byUser.containsKey(String.valueOf(context.caller.uid))) {
                    throw new GatewayException(ApiReturnCode.REQUIRE_VERIFY_CODE_BY_USER_ID);
                }

                if (requestVerifyCodeListeners.byDevice.containsKey(String.valueOf(context.caller.deviceId))) {
                    throw new GatewayException(ApiReturnCode.REQUIRE_VERIFY_CODE_BY_DEVICE_ID);
                }

                if (context.caller.phoneNumber != null &&
                    requestVerifyCodeListeners.byPhonePrefix.prefixContainsKey(context.caller.phoneNumber)) {
                    throw new GatewayException(ApiReturnCode.REQUIRE_VERIFY_CODE_BY_PHONE_PREFIX);
                }
            }
        }
    }
}
