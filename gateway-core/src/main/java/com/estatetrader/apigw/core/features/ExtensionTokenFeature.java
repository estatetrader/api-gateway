package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.JSON;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.define.*;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.apigw.core.phases.executing.request.RequestFinished;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.phases.parsing.ApiRegister;
import com.estatetrader.apigw.core.phases.parsing.ApiVerifier;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.apigw.core.support.CookieSupport;
import com.estatetrader.apigw.core.utils.RsaExtensionTokenHelper;
import com.estatetrader.core.GatewayException;
import com.estatetrader.core.IllegalConfigException;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.entity.ExtensionCallerInfo;
import com.estatetrader.util.Lambda;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.annotation.ExtensionAutowired;
import com.estatetrader.annotation.ExtensionParamsAutowired;
import com.estatetrader.annotation.ExtensionTokenIssuer;
import com.estatetrader.responseEntity.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 扩展相关功能，用于签发、验证及注入etk信息
 *
 * etk可以不传，但是如果传了，则会校验其是否过期，并在过期的时候返回错误码提醒客户端重新获取etk
 * etk的字段名称是全局的，没有命名空间
 */
public interface ExtensionTokenFeature {

    @Component
    class Config {
        @SuppressWarnings("FieldMayBeFinal")
        @Value("${com.estatetrader.extension.token.check.enabled:true}")
        private boolean featureEnabled = true;

        private volatile Map<String, RsaExtensionTokenHelper> subSystemRsaHelperMap = Collections.emptyMap();

        @Value("${com.estatetrader.subsystem.rsaPublicKeys:}")
        public void setSubSystemRsaPublicKeys(String subSystemRsaPublicKeys) {
            Map<String, RsaExtensionTokenHelper> map = new HashMap<>();

            for (String pair : subSystemRsaPublicKeys.split(";")) {
                if (pair.isEmpty()) {
                    continue;
                }

                String[] np = pair.split(":");
                if (np.length != 2) {
                    throw new IllegalArgumentException(pair + " is invalid, must be name:password format.");
                }

                map.put(np[0], new RsaExtensionTokenHelper(np[1]));
            }

            this.subSystemRsaHelperMap = map;
        }

        RsaExtensionTokenHelper getSubsystemExtensionTokenHelper(String subsystem) {
            return subSystemRsaHelperMap.get(subsystem);
        }

        private volatile RsaExtensionTokenHelper extensionTokenHelper;

        @Value("${com.estatetrader.extension.token.public.key}")
        public void setExtensionTokenPublicKey(String extensionTokenPublicKey) {
            this.extensionTokenHelper = new RsaExtensionTokenHelper(extensionTokenPublicKey);
        }

        RsaExtensionTokenHelper getExtensionTokenHelper() {
            return extensionTokenHelper;
        }
    }

    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {
        @Override
        public void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo apiInfo, ServiceInstance serviceInstance) {
            ExtensionTokenIssuer issuer = method.getAnnotation(ExtensionTokenIssuer.class);
            if (issuer == null) {
                return;
            }

            if (issuer.appIds().length == 0) {
                throw new IllegalApiDefinitionException("appIds of @ExtensionTokenIssuer must not be empty");
            }

            String mainField = issuer.mainField().isEmpty() ? null : issuer.mainField();
            apiInfo.etkIssuerInfo = new ExtensionTokenIssuerInfo(issuer.appIds(),
                Arrays.asList(issuer.fields()), mainField);
            
            apiInfo.recordResult = true;
        }
    }

    @Extension
    class ParameterInfoParserImpl implements ParsingClass.ParameterInfoParser {
        @Override
        public boolean parse(Class<?> clazz,
                             Method method,
                             ApiMethodInfo apiInfo,
                             Parameter parameter,
                             ApiParameterInfo pInfo) {

            ExtensionAutowired ea = parameter.getAnnotation(ExtensionAutowired.class);
            if (ea != null) {
                pInfo.serverOnly = true;
                pInfo.name = CommonParameter.extensionAutowiredPrefix + ea.value();
                pInfo.isAutowired = true;

                if (ea.required()) {
                    pInfo.isRequired = true;
                    apiInfo.requiredExtensionToken = true;
                }

                return true;
            }

            ExtensionParamsAutowired ep = parameter.getAnnotation(ExtensionParamsAutowired.class);
            if (ep != null) {
                pInfo.serverOnly = true;
                pInfo.name = CommonParameter.extensionParamsAutowired;
                pInfo.isAutowired = true;

                if (ep.required()) {
                    pInfo.isRequired = true;
                    apiInfo.requiredExtensionToken = true;
                }

                if (!Map.class.isAssignableFrom(parameter.getType())) {
                    throw new IllegalApiDefinitionException("parameter " + pInfo.name + " must be of Map type");
                }

                return true;
            }

            return false;
        }
    }

    @Extension
    class ExtensionIssuerRegister implements ApiRegister {
        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            if (info.etkIssuerInfo == null) {
                return;
            }

            for (int appId : info.etkIssuerInfo.allowedAppIds) {
                if (schema.etkIssuers.put(appId, info) != null) {
                    throw new IllegalApiDefinitionException("@ExtensionTokenIssuer must not be " +
                        "duplicated in app " + appId);
                }
            }
        }
    }

    @Extension
    class ApiVerifyImpl implements ApiVerifier {

        // private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionTokenFeature.class);

        @Override
        public void verify(ApiMethodInfo info, ApiSchema schema) {
            for (ApiParameterInfo p : info.parameterInfos) {
                boolean useEtkField = p.name != null && p.name.startsWith(CommonParameter.extensionAutowiredPrefix);

                if (useEtkField) {
                    String field = p.name.substring(CommonParameter.extensionAutowiredPrefix.length());
                    checkIfExtensionParamIsDefined(info.methodName, field, schema);
                }


                // todo 在线下店各设备上线新的签名之后启用以下检查
                // boolean useEtk = useEtkField || CommonParameter.extensionParamsAutowired.equals(p.name);
//                if (useEtk && info.securityLevel == SecurityType.Anonym) {
//                    throw new IllegalApiDefinitionException("Anonymous API could not use ETK autowired parameters");
//                }
            }
        }

        private void checkIfExtensionParamIsDefined(@SuppressWarnings("unused") String apiName,
                                                    String paramName,
                                                    ApiSchema schema) {
            for (ApiMethodInfo method : schema.etkIssuers.values()) {
                if (method.etkIssuerInfo.isFieldAllowed(paramName)) {
                    return;
                }
            }

            // TODO throws exceptions to enable force-check
//            if (ApiConfig.getInstance().isDevVersion()) {
//                throw new IllegalApiDefinitionException("ETK autowired parameter " + paramName + " is not defined");
//            }

            // LOGGER.info("ETK autowired parameter {} in api {} is not defined", apiName, paramName);
        }
    }

    @Extension
    class ParameterParserImpl implements RequestStarted.ParameterParser {
        @Override
        public void parse(ApiContext context) {
            context.extensionToken = context.getRequest().getParameter(CommonParameter.extensionToken);
            if (context.extensionToken == null || context.extensionToken.isEmpty()) {
                context.extensionToken = context.getRequest().getHeader(CommonParameter.extensionToken);
            }
        }
    }

    @Extension
    class CookieParserImpl implements RequestStarted.CookieParser {

        private static final Logger logger = LoggerFactory.getLogger(ExtensionTokenFeature.class);

        @Override
        public void parse(ApiContext context) {
            String cookie = context.request.getCookieValue(context.appId + CookieName.etoken);

            try {
                if (context.extensionToken == null && cookie != null && cookie.length() > 0) {
                    context.extensionToken = URLDecoder.decode(cookie, StandardCharsets.UTF_8.name());
                }
            } catch (Exception e) {
                logger.error("etoken in cookie error " + cookie, e);
                context.clearExtensionToken = true;
            }
        }
    }

    @Extension
    class AsyncTokenProcessorImpl implements SecurityFeature.AsyncTokenProcessor {

        private static final Logger logger = LoggerFactory.getLogger(ExtensionTokenFeature.class);

        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) {
            if (context.extensionToken == null ||
                context.extensionToken.isEmpty()) {
                return;
            }

            context.extCaller = parseExtensionCallerInfo(context, context.extensionToken);

            if (context.extCaller != null) {
                MDC.put(CommonParameter.extensionUserId, String.valueOf(context.extCaller.eid));
            }
        }

        private ExtensionCallerInfo parseExtensionCallerInfo(ApiContext context, String etoken) {
            context.requestInfo.put("__etk", etoken);

            ExtensionCallerInfo extCaller = RsaExtensionTokenHelper.parseToken(etoken);
            if (extCaller == null) {
                logger.warn("parse etoken failed: {}", etoken);
                context.clearExtensionToken = true;
                return null;
            }

            return extCaller;
        }
    }

    @Extension(after = SecurityFeature.class)
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {

        private static final Logger logger = LoggerFactory.getLogger(ExtensionTokenFeature.class);
        // private static final Logger compLogger = LoggerFactory.getLogger("compatibility-logger");

        private final Config config;

        public RequestVerifierImpl(Config config) {
            this.config = config;
        }

        @Override
        public void verify(ApiContext context) throws GatewayException {
            verifyEtkIssuer(context);
            verifyExtensionCaller(context);
        }

        private void verifyEtkIssuer(ApiContext context) throws GatewayException {
            for (ApiMethodCall call : context.apiCalls) {
                if (call.method.etkIssuerInfo != null && !call.method.etkIssuerInfo.isAppAllowed(context.appId)) {
                    throw new GatewayException(ApiReturnCode.APP_ID_NOT_IN_ETK_ISSUER);
                }
            }
        }

        private void verifyExtensionCaller(ApiContext context) throws GatewayException {
            boolean requiredExtensionToken = Lambda.any(context.apiCalls, c -> c.method.requiredExtensionToken);
            ExtensionCallerInfo extCaller = context.extCaller;

            /*
             * 在大多数场景下请求都不需要注入etk。
             * 如果本次请求不需要注入etk，并且etk不存在，则直接结束校验，快速返回
             */
            if (extCaller == null && !requiredExtensionToken) {
                return;
            }

            CallerInfo caller = context.caller;

            AbstractReturnCode errorCode;
            boolean fatal;

            if (extCaller == null) { // 同时本次请求需要注入etk，则报告etk缺失错误
                errorCode = ApiReturnCode.MISSING_EXTENSION_TOKEN;
                fatal = false;
                // todo 在user-service切换到新的etk时，启用以下校验
//            } else if (extCaller.subsystem == null) {
//                errorCode = ApiReturnCode.EXTENSION_INVALID_SUBSYSTEM;
//                fatal = true;
            } else if (!isExtensionTokenSignatureValid(context.extensionToken, context)) {
                errorCode = ApiReturnCode.EXTENSION_INVALID_SIGNATURE;
                fatal = true;
            } else if (extCaller.expire < System.currentTimeMillis()) {
                errorCode = ApiReturnCode.EXTENSION_TOKEN_EXPIRE;
                fatal = false;
            } else if (caller == null) {
                errorCode = ApiReturnCode.EXTENSION_TOKEN_MISSING_TOKEN;
                fatal = false;
                // todo 在user-service切换到新的etk时，启用以下校验
//            } else if (!extCaller.subsystem.equalsIgnoreCase(caller.subsystem)) {
//                errorCode = ApiReturnCode.EXTENSION_SUBSYSTEM_MISMATCH;
//                fatal = true;
            } else if (extCaller.uid != caller.uid) {
                errorCode = ApiReturnCode.EXTENSION_UID_MISMATCH;
                fatal = true;
            } else if (extCaller.aid != caller.appid) {
                errorCode = ApiReturnCode.EXTENSION_APP_ID_MISMATCH;
                fatal = true;
            } else if (!isRequiredParamsFulfilled(context, extCaller)) {
                errorCode = ApiReturnCode.MISSING_EXTENSION_PARAM;
                fatal = false;
            } else {
                return;
            }

            /*
             * 如果etk校验不通过，无论是否拒绝本次请求，
             * 都会通过返回值（renewExtensionToken）告知客户端它拥有的etk不合法，需要重新获取
             */
            context.clearExtensionToken = true;

            if (config.featureEnabled) {
                context.extCaller = null;
                // 仅在此次请求的确需要etk时才拒绝客户端请求
                if (requiredExtensionToken || fatal) {
                    throw new GatewayException(errorCode);
                }
            }

            logger.warn("etk {} is invalid deal to code {}", context.extensionToken, errorCode);
        }

        private boolean isExtensionTokenSignatureValid(String etk, ApiContext context) {

            RsaExtensionTokenHelper rsaHelper = config.getExtensionTokenHelper();
            if (rsaHelper == null) {
                throw new IllegalConfigException("com.estatetrader.extension.token.public.key",
                    "extension public key must not be null");
            }

            if (rsaHelper.verifyToken(etk)) {
                return true;
            }

            String subsystem = context.extCaller.subsystem;
            if (subsystem == null && context.caller != null) {
                subsystem = context.caller.subsystem;
            }

            if (subsystem == null) {
                logger.warn("subsystem is not defined");
                return false;
            }

            rsaHelper = config.getSubsystemExtensionTokenHelper(subsystem.toLowerCase());

            if (rsaHelper != null && rsaHelper.verifyToken(etk)) {
                logger.warn("subsystem specified etk rsa is deprecated, please use global rsa instead");
                return true;
            }

            return false;
        }

        private boolean isRequiredParamsFulfilled(ApiContext context, ExtensionCallerInfo extCaller ) {
            for (ApiMethodCall call : context.apiCalls) {
                if (call.method.requiredExtensionToken) {
                    for (ApiParameterInfo pInfo : call.method.parameterInfos) {
                        if (!pInfo.isRequired) {
                            continue; // 仅检查必填参数
                        }

                        if (pInfo.name != null && pInfo.name.startsWith(CommonParameter.extensionAutowiredPrefix)) {
                            if (extCaller.parameters == null) {
                                return false;
                            }

                            String field = pInfo.name.substring(CommonParameter.extensionAutowiredPrefix.length());
                            if (!extCaller.parameters.containsKey(field)) {
                                return false;
                            }
                        }
                    }
                }
            }

            return true;
        }
    }

    // 防止SecurityFeature覆盖掉我们的自动注入
    // 仅在客户端没有传递_scp时使用etk注入scp参数
    @Extension(before = SecurityFeature.class, after = SubsystemParametersFeature.class)
    class AutowiredParameterValueProviderImpl implements CallStarted.AutowiredParameterValueProvider {

        // private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);
        private static final String INVALID_ID = String.valueOf(Long.MIN_VALUE);

        /**
         * 注入可注入的参数
         * <p>
         * 根据call和info判断你是否能够为当前参数提供注入，并将最终注入结果作为返回值返回出去
         * 如果你不能为当前参数提供注入，则调用next.go()将机会留给其他注入器
         *
         * @param call    当前待注入的API call
         * @param info    当前待注入的参数信息
         * @param context 请求上下文
         * @param next    如果你需要其他注入器提供注入，则调用next.go()
         * @return 参数最终要注入的值
         * @throws GatewayException 抛出错误码
         */
        @Override
        public String autowire(ApiMethodCall call,
                               ApiParameterInfo info,
                               ApiContext context,
                               Next<String, GatewayException> next) throws GatewayException {

            if (CommonParameter.extensionUserId.equals(info.name)) {
                return context.extCaller == null ? INVALID_ID : String.valueOf(context.extCaller.eid);
            }

            if (CommonParameter.extensionParamsAutowired.equals(info.name)) {
                return JSON.toJSONString(context.extCaller == null ?
                    Collections.emptyMap() : context.extCaller.parameters);
            }

            if (info.name != null && info.name.startsWith(CommonParameter.extensionAutowiredPrefix)) {
                if (context.extCaller == null) {
                    return null;
                }

                String field = info.name.substring(CommonParameter.extensionAutowiredPrefix.length());
                return context.extCaller.parameters.get(field);
            }

            // 仅用于临时兼容，在客户端没有传递 _scp时使用etk注入_scp，后期会删掉整个_scp注入逻辑
            if (CommonParameter.subsystemParams.equals(info.name)) {
                return JSON.toJSONString(context.extCaller == null ?
                    Collections.emptyMap() : context.extCaller.parameters);
            }

            //noinspection deprecation
            if (CommonParameter.subSystemMainId.equals(info.name)) {
                // 用于兼容老版本的utk
                if (context.caller != null && context.caller.subSystemMainId != Long.MIN_VALUE) {
                    return String.valueOf(context.caller.subSystemMainId);
                }

                if (context.extCaller == null || context.extCaller.parameters == null) {
                    return INVALID_ID;
                }

                // 如果dtk/utk中的subsystemMainId未定义，则使用etk中的mainField
                int aid = context.caller != null ? context.caller.appid : context.appId;
                ApiMethodInfo issuerApi = context.apiSchema.etkIssuers.get(aid);
                if (issuerApi != null && issuerApi.etkIssuerInfo.mainField != null) {
                    String value = context.extCaller.parameters.get(issuerApi.etkIssuerInfo.mainField);
                    if (value != null) {
                        return value;
                    }
                }

                return INVALID_ID;
            }

            return next.go();
        }
    }

    @Extension
    class NotificationProcessorImpl implements CallResultReceived.NotificationProcessor, CookieSupport {

        private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);

        /**
         * 处理来自后端服务器返回的旁路信息
         * 如果你对某个信息不感兴趣，请调用next.go()让其他处理器处理
         *
         * @param name    旁路信息的名称
         * @param value   信息的内容
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param next    调用next.go()让其他旁路信息处理器处理
         * @throws IOException 抛出异常
         */
        @Override
        public void process(String name, String value,
                            ApiContext context,
                            ApiMethodCall call,
                            Next.NoResult<IOException> next) throws IOException {

            if (ConstField.SET_COOKIE_ETOKEN.equals(name)) {
                setETokenCookie(context, value);
                return;
            }

            next.go();
        }

        private void setETokenCookie(ApiContext apiContext, String value)
            throws IOException {

            String[] etkInfos;
            if (value != null && value.length() > 0 && (etkInfos = value.split("\\|")).length > 1) {
                int duration = -1;
                String etk = value;
                int appId = apiContext.appId;

                try {
                    etk = etkInfos[0];
                    duration = Integer.parseInt(etkInfos[1]);
                    if (apiContext.caller != null && duration > apiContext.caller.expire) {
                        duration = (int) apiContext.caller.expire;
                    }
                    if(etkInfos.length > 2) {
                        appId = Integer.parseInt(etkInfos[2]);
                    }
                } catch (Exception e) {
                    logger.error("parse etk expire time error." + value, e);
                }

                int finalDuration = duration;

                dispatchCookie(apiContext.response, appId + CookieName.etoken, URLEncoder.encode(etk, StandardCharsets.UTF_8.name()), c -> {
                    c.setMaxAge(finalDuration);
                    c.setHttpOnly(true);
                    c.setDomain(apiContext.host);
                });

                dispatchCookie(apiContext.response, appId + CookieName.extTokenExist, "1", c -> {
                    c.setMaxAge(finalDuration);
                    c.setDomain(apiContext.host);
                });
            } else {
                logger.info("clear etoken");
                apiContext.clearExtensionToken = true;
            }
        }
    }

    @Extension(after = SecurityFeature.class)
    class CookieDispatcherImpl implements RequestFinished.CookieDispatcher, CookieSupport {
        @Override
        public void dispatch(ApiContext context) {
            if (!context.userTokenExpired && !context.clearExtensionToken) {
                return;
            }

            GatewayResponse response = context.getResponse();

            dispatchCookie(response, context.appId + CookieName.etoken, c -> {
                c.setMaxAge(0);
                c.setDomain(context.host);
            });

            dispatchCookie(response, context.appId + CookieName.extTokenExist, c -> {
                c.setMaxAge(0);
                c.setDomain(context.host);
            });
        }
    }

    @Extension
    class ResponseGeneratorImpl implements RequestFinished.ResponseGenerator {
        @Override
        public void process(ApiContext context,
                            AbstractReturnCode code,
                            List<ApiMethodCall> calls,
                            Response response) {

            if (context.clearExtensionToken) {
                response.needRenewExtensionToken = true;
            }
        }
    }
}
