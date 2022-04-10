package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.core.GatewayException;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.rule.cases.BlacklistsService;
import com.estatetrader.apigw.core.models.ApiContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 黑名单相关功能
 */
public interface BlacklistFeature {

    @Component
    class Config {
        @SuppressWarnings("FieldMayBeFinal")
        @Value("${com.estatetrader.apigw.enableBlacklist:true}")
        private boolean featureEnabled = true;
    }

    @Extension(after = SecurityFeature.class)
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {

        private final BlacklistsService.Listeners blacklistsListeners;
        private final Config config;

        public RequestVerifierImpl(@Autowired(required = false)
                                       BlacklistsService.Listeners blacklistsListeners,
                                   Config config) {
            this.blacklistsListeners = blacklistsListeners;
            this.config = config;
        }

        @Override
        public void verify(ApiContext context) throws GatewayException {
            if (!config.featureEnabled ||
                context.caller == null ||
                blacklistsListeners == null) {
                return;
            }

            if (blacklistsListeners.userBlacklist.containsKey(String.valueOf(context.caller.uid))) {
                throw new GatewayException(ApiReturnCode.USER_BLACKLIST_DENIED);
            }

            if (blacklistsListeners.deviceBlacklist.containsKey(String.valueOf(context.caller.deviceId))) {
                throw new GatewayException(ApiReturnCode.DEVICE_BLACKLIST_DENIED);
            }

            if (context.clientIP != null &&
                blacklistsListeners.ipBlacklist.containsKey(context.clientIP)) {

                throw new GatewayException(ApiReturnCode.IP_BLACKLIST_DENIED);
            }

            if (context.caller.phoneNumber != null &&
                blacklistsListeners.phonePrefixBlacklist.prefixContainsKey(context.caller.phoneNumber)) {

                throw new GatewayException(ApiReturnCode.PHONE_PREFIX_BLACKLIST_DENIED);
            }
        }
    }
}
