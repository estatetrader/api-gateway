package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.JSON;
import com.estatetrader.annotation.ApiSubSystem;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.rule.WatchedResourceListener;
import com.estatetrader.rule.authorizing.ApiInfo;
import com.estatetrader.rule.authorizing.AuthorizationTreeListener;
import com.estatetrader.rule.authorizing.SubsystemInfo;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 授权树相关功能
 */
public interface AuthorizationFeature {

    @Component
    class Config {
        @SuppressWarnings("FieldMayBeFinal")
        @Value("${com.estatetrader.authorization.tree.enabled:true}")
        private boolean featureEnabled = true;

        private Set<String> subsystemsToSkipAuthorization = Collections.emptySet();

        @Value("${com.estatetrader.authorization.subsystems.to.skip:}")
        public void setSubsystemsToSkipAuthorization(String value) {
            if (value == null || value.isEmpty()) {
                subsystemsToSkipAuthorization = Collections.emptySet();
                return;
            }

            subsystemsToSkipAuthorization = new HashSet<>(Arrays.asList(value.split(" *, *")));
        }
    }

    @Extension(after = SecurityFeature.class)
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {
        @Override
        public void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo apiInfo, ServiceInstance serviceInstance) {
            @SuppressWarnings("deprecation")
            ApiSubSystem a = method.getAnnotation(ApiSubSystem.class);
            if (a != null) {
                apiInfo.subSystem = a.value().toLowerCase();
                apiInfo.needVerifyAuthorization = a.authorizing() && apiInfo.securityLevel != SecurityType.Anonym;
            }

            if (SecurityType.AuthorizedUser.check(apiInfo.securityLevel)) {
                apiInfo.needVerifyAuthorization = true;
            }
        }
    }

    @Extension(after = SecurityFeature.class)
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {

        private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationFeature.class);

        private final AuthorizationTreeListener authorizationTreeListener;
        private final WatchedResourceListener<?> superUserListener;
        private final Config config;

        public RequestVerifierImpl(@Autowired(required = false) AuthorizationTreeListener authorizationTreeListener,
                                   @Autowired(required = false) WatchedResourceListener<?> superUserListener,
                                   Config config) {
            this.superUserListener = superUserListener;
            this.authorizationTreeListener = authorizationTreeListener;
            this.config = config;
        }

        @Override
        public void verify(ApiContext context) throws GatewayException {
            for (ApiMethodCall call : context.apiCalls) {
                checkAuthorization(context, call.method);
            }
        }

        private void checkAuthorization(ApiContext context, ApiMethodInfo method) throws GatewayException {
            if (!method.needVerifyAuthorization) {
                return;
            }

            if (context.caller == null) {
                throw new IllegalStateException("context.caller must be checked in SecurityFeature");
            }

            String role = context.caller.role;

            // TODO 待移除 作为接口权限的验证点, role 不应该从ext中获取. 当前用户必须在当前子系统拥有这个接口的调用权限
            if (context.extCaller != null && context.extCaller.role != null) {
                role = context.extCaller.role;
            }

            if (superUserListener != null &&
                superUserListener.containsKey(String.valueOf(context.caller.uid))) {
                LOGGER.debug("authorizing is skipped for super user {}", context.caller.uid);
                return;
            }

            // 子系统的名称，我们校验凭据中的子系统下的角色是否能访问这个子系统对应的权限树
            if (context.caller.subsystem == null || context.caller.subsystem.isEmpty()) {
                // 【白名单授权机制】仅允许包含合法子系统的token才允许访问授权的API
                throw new GatewayException(ApiReturnCode.TOKEN_MISSING_SUBSYSTEM);
            }

            boolean checkAuth = config.featureEnabled && // 全局权限校验开关
                // 针对特定子系统的权限校验跳过
                !config.subsystemsToSkipAuthorization.contains(context.caller.subsystem.toUpperCase()) &&
                // 针对特定平台的权限校验跳过
                !config.subsystemsToSkipAuthorization.contains(String.valueOf(context.caller.appid));

            if (checkAuth) {
                checkAuthorizationTree(context, method, role);
            } else {
                try {
                    checkAuthorizationTree(context, method, role);
                } catch (GatewayException e) {
                    LOGGER.warn("check authorization to access {} failed to role {}, subsystem = {}, appId = {}: {}",
                        method.methodName,
                        context.caller.role,
                        context.caller.subsystem,
                        context.caller.appid,
                        e.getCode());
                }
            }
        }

        private void checkAuthorizationTree(ApiContext context, ApiMethodInfo method, String role) throws GatewayException {
            AuthorizationTreeListener listener = authorizationTreeListener;
            if (listener == null) {
                return;
            }

            // context.caller.subsystem is not null or empty, confirmed by the method's caller
            String subsystemName = context.caller.subsystem.toUpperCase();

            SubsystemInfo subsystem = listener.getSubsystem(subsystemName);

            // 【白名单授权机制】如果子系统节点不存在，则拒绝授权
            // 为了防止因为凭据签发相关bug（因拼写错误导致签发了一个不存在的子系统的凭据），我们采用了白名单机制，即默认所有子系统
            // 的凭据都被拒绝访问，除非显式在权限树中有相应配置
            if (subsystem == null) {
                throw new GatewayException(ApiReturnCode.SUBSYSTEM_NOT_FOUND);
            }

            // 指示该凭据中指示的角色仅在可信赖网络中使用
            if (subsystem.isOnlyTrustedNetwork() && !context.fromTrustedNetwork) {
                LOGGER.debug("authorizing is rejected since the subsystem {} is only allowed from trusted network",
                    subsystemName);

                throw new GatewayException(ApiReturnCode.CLIENT_IP_DENIED);
            }

            // 要访问的API的名字，是权限校验的基本单元
            String apiName = method.methodName;

            ApiInfo api = listener.getApi(subsystemName, apiName);

            // 【白名单授权机制】如果API未在此子系统节点下挂载，则拒绝请求
            if (api == null) {
                throw new GatewayException(ApiReturnCode.PERMISSION_UNASSIGNED);
            }

            // 如果子系统被配置不启用校验，则跳过权限校验
            if (subsystem.isAuthorizingDisabled()) {
                LOGGER.debug("authorizing check skipped because authorizing is not enabled for subsystem {}",
                    subsystemName);

                return;
            }

            // API可以选择是否启用API权限校验
            if (api.isAuthorizingDisabled()) {
                LOGGER.debug("authorizing check skipped because the authorizing of the target api {} " +
                    "in subsystem {} authorizing tree is disabled", apiName, subsystemName);

                return;
            }

            if (listener.containsApiRole(subsystemName, apiName, role)) {
                LOGGER.debug("access to api {} in subsystem {} by role {} is granted", apiName, subsystemName, role);
                return;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("authorizing tree {}.{} = {} does not contain role {}",
                    subsystemName, apiName, JSON.toJSONString(listener.getApiRoles(subsystemName, apiName)), role);
            }

            // 报告前端权限不足
            throw new GatewayException(ApiReturnCode.PERMISSION_CHECK_FAILED);
        }
    }
}
