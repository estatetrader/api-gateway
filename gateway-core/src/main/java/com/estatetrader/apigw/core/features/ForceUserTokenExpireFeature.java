package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.rule.expire.ExpireReason;
import com.estatetrader.rule.expire.ExpiredUserTokenListener;
import com.estatetrader.rule.expire.UserTokenExpireRule;
import com.estatetrader.util.Lambda;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.models.ApiContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 强制utk过期
 *
 * 根据zk中给定的规则，将满足条件的utk强制过期掉
 *
 * 具体表现为，如果需要强制过期一个utk，则将其过期时间设置为当前时间-1
 * 后续交由UserTokenExpireFeature完成实际的过期逻辑
 */
public interface ForceUserTokenExpireFeature {

    @Component
    class Config {
        @SuppressWarnings("FieldMayBeFinal")
        @Value("${com.estatetrader.feature.expire.token.enabled:true}")
        private boolean featureEnabled = true;
    }

    // 如果utk已过期，则使用自动renew之后的utk
    @Extension(before = UserTokenExpireFeature.class, after = RenewUserTokenFeature.class)
    class AsyncTokenProcessorImpl implements SecurityFeature.AsyncTokenProcessor {

        private final Extensions<RenewUserTokenFeature.RenewUserTokenOperator> renewUserTokenProviders;
        private final ExpiredUserTokenListener expiredUserTokenListener;
        private final Config config;

        public AsyncTokenProcessorImpl(Extensions<RenewUserTokenFeature.RenewUserTokenOperator> renewUserTokenProviders,
                                       @Autowired(required = false)
                                           ExpiredUserTokenListener expiredUserTokenListener,
                                       Config config) {

            this.renewUserTokenProviders = renewUserTokenProviders;
            this.expiredUserTokenListener = expiredUserTokenListener;
            this.config = config;
        }

        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) throws GatewayException {
            if (context.caller == null ||
                !SecurityType.requireToken(context.requiredSecurity) || // 仅在此次请求需要tk时验证
                context.caller.uid == 0 || // non-user token
                context.caller.securityLevel == 0 || // non-user token
                expiredUserTokenListener == null || // service is not defined
                !config.featureEnabled // feature is disabled
            ) {
                return;
            }

            ExpireReason reason = checkRules(context.token, context.caller,
                expiredUserTokenListener.getRulesForAllUsers());

            if (reason == null) {
                reason = checkRules(context.token, context.caller,
                    expiredUserTokenListener.getRulesForUser(context.caller.uid));
            }

            if (reason == null) {
                return;
            }

            context.caller.expire = context.startTime - 1; // 强制token过期
            context.userTokenExpireReason = reason; // 并记录过期原因

            if (reason.tryToRenew) {
                // renew函数会自动避免重复renew
                // 如果renew成功，我们会得到一个不过期的utk，所以context.userTokenExpireReason便不会起作用
                renew(context, pipeline);
            }
        }

        private void renew(ApiContext context, WorkflowPipeline pipeline) throws GatewayException {
            renewUserTokenProviders.chain(RenewUserTokenFeature.RenewUserTokenOperator::renew,
                context, pipeline).go();
        }

        private ExpireReason checkRules(String token, CallerInfo caller, List<UserTokenExpireRule> rules) {
            if (rules == null || rules.isEmpty()) {
                return null;
            }

            for (UserTokenExpireRule rule : rules) {
                // 多个rule之间的关系为"或"
                if (checkRule(token, caller, rule)) {
                    // reason默认值为ExpireReason.EXPIRED
                    return Lambda.cascade(rule.getReason(), ExpireReason.EXPIRED);
                }
            }

            return null;
        }

        private boolean checkRule(String token, CallerInfo caller, UserTokenExpireRule rule) {
            /*
             * 对rule的各个字段进行检查，这些字段之间的关系为"且"
             */

            if (rule.getBeforeTime() <= 0 || // 排除rule.beforeTime不合法规则
                rule.getBeforeTime() <= caller.createdTime) { // 不满足最晚签发时间规则
                // 老版本的utk中没有createdTime，其值为0，因此老版本utk的签发时间早于任何规则给定的时间
                return false;
            }

            if (rule.getToken() != null && !rule.getToken().equals(token)) {
                // 针对特定token的判断
                return false;
            }

            if (rule.getAppId() != null && rule.getAppId() != caller.appid) {
                // 针对特定app的判断
                return false;
            }

            if (rule.getSubsystem() != null && !rule.getSubsystem().equalsIgnoreCase(caller.subsystem)) {
                // 针对特定subsystem的判断
                return false;
            }

            //noinspection RedundantIfStatement
            if (rule.getRole() != null && !rule.getRole().equals(caller.role)) {
                return false;
            }

            // 至此，token满足rule定义的所有规则
            return true;
        }
    }
}
