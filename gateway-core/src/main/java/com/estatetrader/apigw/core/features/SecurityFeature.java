package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.JSON;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.ConstField;
import com.estatetrader.define.CookieName;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.entity.CallerInfo;
import com.estatetrader.util.AESTokenHelper;
import com.estatetrader.util.Lambda;
import com.estatetrader.util.RawString;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.annotation.HttpApi;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.apigw.core.phases.executing.request.RequestFinished;
import com.estatetrader.apigw.core.support.ApiMDCSupport;
import com.estatetrader.apigw.core.support.CookieSupport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 安全相关功能，包含身份认证功能，而权限校验相关已拆分到AuthorizationFeature中
 */
public interface SecurityFeature {

    @Component
    class Config {
        @SuppressWarnings("FieldMayBeFinal")
        @Value("${com.estatetrader.internal.ip.check.enabled:true}")
        private boolean internalIPCheckEnabled = true;

        private final AESTokenHelper aesTokenHelper;

        public Config(@Value("${com.estatetrader.apigw.tokenAes}") String apiTokenAes) {
            this.aesTokenHelper = new AESTokenHelper(apiTokenAes);
        }

        AESTokenHelper getAesTokenHelper() {
            return aesTokenHelper;
        }

        private volatile Map<String, String> originWhiteList = Collections.emptyMap();

        @Value("${com.estatetrader.originWhiteList:}")
        public void setOriginWhiteList(String list) {
            Map<String, String> map = new HashMap<>();
            if (list != null && list.length() > 0) {
                String[] os = list.split(",");
                for (String o : os) {
                    String domain = o.trim();
                    int index = domain.lastIndexOf('.', domain.lastIndexOf('.') - 1);
                    map.put(domain, index != -1 ? domain.substring(index) : domain);
                }
            }
            this.originWhiteList = map;
        }

        public Map<String, String> getOriginWhiteList() {
            return originWhiteList;
        }

        private volatile Set<Integer> appsDisableTokenCookies = Collections.emptySet();

        @Value("${gateway.apps-not-use-token-cookies:}")
        public void setAppsDisableTokenCookies(String value) {
            Set<Integer> newValue;
            if (value == null || value.isEmpty()) {
                newValue = Collections.emptySet();
            } else {
                newValue = new HashSet<>();
                for (String s : value.split(" *, *")) {
                    if (!s.isEmpty()) {
                        newValue.add(Integer.parseInt(s));
                    }
                }
            }
            this.appsDisableTokenCookies = newValue;
        }

        Set<Integer> getAppsDisableTokenCookies() {
            return appsDisableTokenCookies;
        }

        @Value("${com.estatetrader.apigw.internalEnvironmentToken}")
        private String internalEnvironmentToken;

        @Value("${com.estatetrader.apigw.trustedNetworkToken:}")
        private String trustedNetworkToken;

        private Set<String> internalIpList = new HashSet<>();
        private Set<String> internalNetSegmentList = new HashSet<>();
        private Set<String> companyIPList = new HashSet<>();

        boolean isInternalIp(String ip) {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            if (internalIpList.contains(ip)) {
                return true;
            } else if (companyIPList.contains(ip)) {
                return true;
            } else if (!internalNetSegmentList.isEmpty()){
                for (String seg : internalNetSegmentList) {
                    if (isInSubNet(ip, seg)) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Value("${com.estatetrader.apigw.internalIpList:}")
        public void setInternalIpList(String internalIpText) {
            Set<String> internalIps = new HashSet<>();
            Set<String> internalNetSegments = new HashSet<>();
            String[] list = StringUtils.split(internalIpText, ',');
            for (String item : list) {
                if (item.contains("/")) {
                    internalNetSegments.add(item);
                } else {
                    internalIps.add(item);
                }
            }

            this.internalIpList = internalIps;
            this.internalNetSegmentList = internalNetSegments;
        }

        @Value("${com.estatetrader.apigw.companyIPList:}")
        public void setCompanyIPList(String companyIPList) {
            this.companyIPList = new HashSet<>(Arrays.asList(StringUtils.split(companyIPList, ',')));
        }

        private static boolean isInSubNet(String network, String seg) {
            if (seg.indexOf('/') < 0) { // ignore non-subnet address
                return false;
            }
            String[] networkips = network.split("\\.");
            int ipAddr = (Integer.parseInt(networkips[0]) << 24)
                | (Integer.parseInt(networkips[1]) << 16)
                | (Integer.parseInt(networkips[2]) << 8)
                | Integer.parseInt(networkips[3]);
            int type = Integer.parseInt(seg.replaceAll(".*/", ""));
            int mask1 = 0xFFFFFFFF << (32 - type);
            String maskIp = seg.replaceAll("/.*", "");
            String[] maskIps = maskIp.split("\\.");
            int cidrIpAddr = (Integer.parseInt(maskIps[0]) << 24)
                | (Integer.parseInt(maskIps[1]) << 16)
                | (Integer.parseInt(maskIps[2]) << 8)
                | Integer.parseInt(maskIps[3]);

            return (ipAddr & mask1) == (cidrIpAddr & mask1);
        }
    }

    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {
        @Override
        public void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo apiInfo, ServiceInstance serviceInstance) {
            apiInfo.securityLevel = method.getAnnotation(HttpApi.class).security();
        }
    }

    @Extension
    class HeaderParserImpl implements RequestStarted.HeaderParser {
        /**
         * 我们通过检测请求中是否含有特定头（以及给定的值）来确认请求是否来源于内网环境
         * 所谓内网环境指的是客户端和服务器端是否属于同一个环境的机器（例如均为生产环境中的机器）
         * 应将其和可信赖网络区分开（X-TRUSTED-NETWORK-TOKEN）
         */
        private static final String INTERNAL_ENVIRONMENT_TOKEN_HEADER = "X-INTERNAL-ENVIRONMENT-TOKEN";
        /**
         * 我们通过检测请求中是否含有特定头（以及给定的值）来确认请求是否来源于可信赖网络环境
         * 所谓可信赖网络环境指的是客户端属于我们信任的网络（例如办公室网络和特定的IP白名单）
         * 应将其和内网环境区分开（X-INTERNAL-ENVIRONMENT-TOKEN）
         */
        private static final String TRUSTED_NETWORK_TOKEN_HEADER = "X-TRUSTED-NETWORK-TOKEN";

        private final Config config;

        public HeaderParserImpl(Config config) {
            this.config = config;
        }

        @Override
        public void parse(ApiContext context) {
            context.fromInternalEnvironment = hasInternalEnvironmentToken(context.getRequest());

            context.fromTrustedNetwork = hasTrustedNetworkToken(context.getRequest()) ||
                config.isInternalIp(context.clientIP);
        }

        private boolean hasInternalEnvironmentToken(GatewayRequest request) {
            String actual = request.getHeader(INTERNAL_ENVIRONMENT_TOKEN_HEADER);
            // logger.debug("X-INTERNAL-ENVIRONMENT-TOKEN: {}", actual);
            return actual != null && actual.equals(config.internalEnvironmentToken);
        }

        private boolean hasTrustedNetworkToken(GatewayRequest request) {
            String actual = request.getHeader(TRUSTED_NETWORK_TOKEN_HEADER);
            // logger.debug("X-TRUSTED-NETWORK-TOKEN: {}", actual);
            return actual != null && actual.equals(config.trustedNetworkToken);
        }
    }

    @Extension
    class ParameterParserImpl implements RequestStarted.ParameterParser {

        private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);

        @Override
        public void parse(ApiContext context) {
            GatewayRequest request = context.getRequest();

            context.token = request.getParameter(CommonParameter.token);
            if (context.token == null || context.token.isEmpty()) {
                context.token = request.getHeader(CommonParameter.token);
            }

            context.deviceToken = request.getParameter(CommonParameter.deviceToken);
            if (context.deviceToken == null || context.deviceToken.isEmpty()) {
                context.deviceToken = request.getHeader(CommonParameter.deviceToken);
            }

            String deviceStr = request.getParameter(CommonParameter.deviceId);
            if (deviceStr != null && !deviceStr.isEmpty()) {
                try {
                    context.deviceId = Long.parseLong(deviceStr);
                } catch (NumberFormatException e) {
                    // 忽略格式不正确对did
                    if (logger.isWarnEnabled()) {
                        logger.warn("invalid did " + deviceStr + " from client", e);
                    }
                }
                MDC.put(CommonParameter.deviceId, deviceStr);
            }
        }
    }

    @Extension
    class CookieParserImpl implements RequestStarted.CookieParser {

        private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);

        private final Config config;

        CookieParserImpl(Config config) {
            this.config = config;
        }

        @Override
        public void parse(ApiContext context) {
            if (config.getAppsDisableTokenCookies().contains(context.appId)) {
                // 屏蔽特定app下的cookie
                return;
            }

            // 优先使用 url 中的 userToken 和 deviceId
            String token = context.request.getCookieValue(context.appId + CookieName.token);
            if (token != null && !token.isEmpty() && context.token == null) {
                try {
                    context.token = URLDecoder.decode(token, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    logger.error("token in cookie error " + token, e);
                    context.userTokenExpired = true;
                }
            }

            String deviceId = context.request.getCookieValue(CookieName.deviceId);
            if (deviceId != null && !deviceId.isEmpty() && context.deviceId == 0) {
                context.deviceId = Long.parseLong(deviceId);
            }
        }
    }

    @Extension
    class ContextProcessorImpl implements RequestStarted.ContextProcessor, CookieSupport {

        private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);

        private final ProcessTokenExecution[] processTokenExecutions;
        private final Config config;

        public ContextProcessorImpl(Extensions<AsyncTokenProcessor> asyncTokenProcessors,
                                    Config config) {
            this.processTokenExecutions = Lambda.map(
                asyncTokenProcessors,
                ProcessTokenExecution::new
            ).toArray(new ProcessTokenExecution[0]);

            this.config = config;
        }

        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) {
            parseToken(context);
            parseDeviceToken(context);
            processSecurity(context);
            pipeline.stage(processTokenExecutions);
        }

        /**
         * 解析调用者身份(在验证签名正确前此身份不受信任)
         */
        private void parseToken(ApiContext context) {
            if (context.token == null) {
                return;
            }

            String bearerPrefix = "Bearer ";

            // remove useless bearer
            if (context.token.startsWith(bearerPrefix)) {
                context.token = context.token.substring(bearerPrefix.length());
            }

            try {
                // user token存在时解析出调用者信息
                context.caller = parseCallerInfo(context.token);
            } catch (Exception e) {
                logger.warn("parse token failed: " + context.token, e);
            }

            if (context.caller != null && context.caller.deviceId != 0 && context.caller.deviceId != -1) {
                context.deviceId = context.caller.deviceId;

                MDC.put(CommonParameter.userId, String.valueOf(context.caller.uid));
            }
        }

        /**
         * 解析调用者身份(在验证签名正确前此身份不受信任)
         */
        private void parseDeviceToken(ApiContext context) {
            if (context.deviceToken == null) {
                return;
            }

            try {
                // device token存在时解析出调用者信息
                context.deviceCaller = parseCallerInfo(context.deviceToken);
            } catch (Exception e) {
                logger.warn("parse token failed: " + context.deviceToken, e);
            }
        }

        /**
         * 从userToken中解析调用者信息
         */
        private CallerInfo parseCallerInfo(String token) {
            CallerInfo caller = null;
            if (token != null && !token.isEmpty()) {
                caller = config.aesTokenHelper.parseToken(token);
            }
            return caller;
        }

        private void processSecurity(ApiContext context) {
            for (ApiMethodCall call : context.apiCalls) {
                context.requiredSecurity = call.method.securityLevel.authorize(context.requiredSecurity);
            }

            //Integrate anon Internal级别接口不具备用户身份
            if (SecurityType.requireToken(context.requiredSecurity)) {
                //默认验证 RegisteredDevice
                context.requiredSecurity = SecurityType.RegisteredDevice.authorize(context.requiredSecurity);
            }
        }
    }

    class ProcessTokenExecution implements WorkflowExecution.Sync, ApiMDCSupport {

        private final AsyncTokenProcessor processor;

        ProcessTokenExecution(AsyncTokenProcessor processor) {
            this.processor = processor;
        }

        /**
         * execute the execution of the node
         *
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
         * @throws Throwable exception occurred while starting this node
         */
        @Override
        public void run(WorkflowPipeline pipeline) throws Throwable {
            ApiContext context = (ApiContext) pipeline.getContext();
            setupMDC(context);
            try {
                processor.process(context, pipeline);
            } finally {
                cleanupMDC();
            }
        }
    }

    interface AsyncTokenProcessor {
        void process(ApiContext context, WorkflowPipeline pipeline) throws GatewayException;
    }

    @Extension
    class RequestVerifierImpl implements RequestStarted.RequestVerifier {

        private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);

        private final Config config;

        public RequestVerifierImpl(Config config) {
            this.config = config;
        }

        @Override
        public void verify(ApiContext context) throws GatewayException {
            checkRequestSecurity(context);
            for (ApiMethodCall call : context.apiCalls) {
                checkExtraSecurityForApi(context, call.method);
            }
        }

        private void checkRequestSecurity(ApiContext context) throws GatewayException {
            int authTarget = context.requiredSecurity;

            if (SecurityType.Internal.check(authTarget) &&
                !context.fromInternalEnvironment &&
                !context.fromTrustedNetwork /* TODO internal级别的API仅应被内网环境的请求访问，此做临时兼容处理 */) {

                if (config.internalIPCheckEnabled) {
                    throw new GatewayException(ApiReturnCode.CLIENT_IP_DENIED);
                } else {
                    logger.error("internal IP check failed for IP {}", context.clientIP);
                }
            }

            if (SecurityType.InternalUser.check(authTarget) &&
                !context.fromTrustedNetwork) {

                if (config.internalIPCheckEnabled) {
                    throw new GatewayException(ApiReturnCode.CLIENT_IP_DENIED);
                } else {
                    logger.error("internal IP check failed for IP {}", context.clientIP);
                }
            }

            CallerInfo caller = context.caller;

            // 当前请求需要utk
            if (SecurityType.requireIdentifiedUserToken(authTarget) &&
                // utk的判断标准: uid > 0 && identified = true
                (caller == null || caller.uid <= 0 || !caller.identified)) {

                if (context.userTokenExpired) {
                    // 如果此次utk缺失的原因是token过期，则报告token过期（前端仍然收到-360）
                    throw new GatewayException(ApiReturnCode.USER_TOKEN_EXPIRE);
                } else {
                    // 提示客户端此次API请求需要utk，引导用户进行第二阶段登录（绑定手机号）
                    throw new GatewayException(ApiReturnCode.USER_TOKEN_ERROR);
                }
            }

            // 当前请求需要ntk
            if (SecurityType.requireUnidentifiedUser(authTarget) &&
                // ntk的判断标准：uid > 0
                (caller == null || caller.uid <= 0)) {
                throw new GatewayException(ApiReturnCode.UNIDENTIFIED_USER_TOKEN_ERROR);
            }

            // 当前请求需要ptk
            if (SecurityType.requirePartnerToken(authTarget) &&
                // ptk的判断标准：partnerBindId != 0
                (caller == null || caller.partnerBindId == 0)) {
                throw new GatewayException(ApiReturnCode.PARTNER_TOKEN_ERROR);
            }

            // 当前请求需要dtk
            if (SecurityType.requireToken(authTarget)) {

                // 所有token均为dtk
                if (caller == null) {
                    throw new GatewayException(ApiReturnCode.DEVICE_TOKEN_ERROR);
                }

                //noinspection deprecation
                if (SecurityType.InternalUser.check(authTarget) &&
                    !SecurityType.InternalUser.check(caller.securityLevel) &&
                    !SecurityType.SubSystem.check(caller.securityLevel)) {
                    // add SecurityType.SubSystem.check(caller.securityLevel)
                    // check to allow subsystem users to visit boss system.
                    throw new GatewayException(ApiReturnCode.ACCESS_DENIED);
                }

                // todo delete these two blocks for if the internal-user and subsystem levels are removed
                //noinspection deprecation
                if (SecurityType.SubSystem.check(authTarget) &&
                    !SecurityType.SubSystem.check(caller.securityLevel)) {
                    throw new GatewayException(ApiReturnCode.ACCESS_DENIED);
                }

                if (context.appId != caller.appid) {
                    logger.warn("appId mismatch. context.appId:{}, caller.appId:{}", context.appId, caller.appid);
                    // throw new GatewayException(ApiReturnCode.APP_ID_MISMATCH);
                }

                if (caller.deviceId == 0) {
                    logger.warn("caller.deviceId is 0");
                    // throw new GatewayException(ApiReturnCode.DEVICE_ID_IS_MISSING);
                }
            }

            // 在token类型判断之后，我们需要深化API权限中的特殊要求
        }

        private void checkExtraSecurityForApi(ApiContext context, ApiMethodInfo method) throws GatewayException {
            // 接口返回RawString，不允许多接口同时调用
            if (method.returnType.equals(RawString.class)) {
                if (context.apiCalls.size() > 1) {
                    throw new GatewayException(ApiReturnCode.ILLEGAL_MUTLI_RAWSTRING_RT);
                }
            }

            // 调用接口中包含了SecurityType为Integrated的接口，不允许多接口同时调用
            if (SecurityType.Integrated.check(method.securityLevel)) {
                if (context.apiCalls.size() > 1) {
                    throw new GatewayException(ApiReturnCode.ILLEGAL_MUTLI_INTEGRATED_API_ACCESS);
                }
            }

            // 本接口只允许加密调用
            if (method.encryptionOnly && !context.httpsMode) {
                throw new GatewayException(ApiReturnCode.UNKNOWN_ENCRYPTION_DENIED);
            }
        }
    }

    @Extension(before = CommonParameterFeature.class)
    class AutowiredParameterValueProviderImpl implements CallStarted.AutowiredParameterValueProvider {

        private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);

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

            switch (info.name) {
                case CommonParameter.userId:
                    return context.caller == null ? "0" : String.valueOf(context.caller.uid);
                case CommonParameter.deviceId:
                    return String.valueOf(context.deviceId);
                case CommonParameter.userIdOrDeviceId:
                    if (context.caller != null && context.caller.uid > 0) {
                        return String.valueOf(context.caller.uid);
                    }
                    if (context.caller != null) {
                        return String.valueOf(context.caller.deviceId);
                    }
                    return String.valueOf(context.deviceId);
                case CommonParameter.partnerBindId:
                    return context.caller == null ? "0" : String.valueOf(context.caller.partnerBindId);
                case CommonParameter.phoneNumber:
                    return context.caller == null ? null : context.caller.phoneNumber;
                case CommonParameter.token:
                    return context.caller == null ? null : context.token;
                case CommonParameter.roleId:
                    return context.caller == null ? null : context.caller.role;
                case CommonParameter.cookie:
                    return getParamForCookieAutowired(context, info);
                default:
                    return next.go();
            }
        }

        private String getParamForCookieAutowired(ApiContext context, ApiParameterInfo ap) {
            GatewayRequest request = context.getRequest();
            Map<String, String> map = new HashMap<>(ap.names.length);

            for (String n : ap.names) {
                String v = null;
                if (CookieName.ttoken.equals(n)) {
                    try {
                        v = context.getCookie(context.appId + n);
                        v = v == null ? null : URLDecoder.decode(v, StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        logger.warn("temp token url decode failed." );
                    }
                    // TODO remove this hot fix and support header inject
                } else if ("androidvname".equals(n)) {
                    v = request.getHeader("androidvname" );
                } else {
                    v = request.getParameter(n.startsWith("_" ) ? n : "_" + n);
                    if (v == null) {
                        v = context.getCookie(n);
                    }
                }
                if (v != null) {
                    map.put(n, v);
                }
            }
            String v = request.getParameter("utm_source");
            if (v!=null&&v.length()>0) {
                map.put("utm_source", v);
            }
            v = request.getParameter("utm_medium");
            if (v!=null&&v.length()>0) {
                map.put("utm_medium", v);
            }
            v = request.getParameter("utm_campaign");
            if (v!=null&&v.length()>0) {
                map.put("utm_campaign", v);
            }
            v = request.getParameter("utm_content");
            if (v!=null&&v.length()>0) {
                map.put("utm_content", v);
            }
            v = request.getParameter("utm_term");
            if (v!=null&&v.length()>0) {
                map.put("utm_term", v);
            }
            
            return JSON.toJSONString(map);
        }
    }

    @Extension
    class NotificationProcessorImpl implements CallResultReceived.NotificationProcessor, CookieSupport {

        private static final Logger logger = LoggerFactory.getLogger(SecurityFeature.class);

        private final Config config;

        NotificationProcessorImpl(Config config) {
            this.config = config;
        }

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
        public void process(String name,
                            String value,
                            ApiContext context,
                            ApiMethodCall call,
                            Next.NoResult<IOException> next) throws IOException {

            switch (name) {
                case ConstField.SET_COOKIE_TTOKEN:
                    setTTokenCookie(context, value);
                    break;
                case ConstField.SET_COOKIE_STOKEN:
                    // do nothing
                    break;
                case ConstField.SET_COOKIE_TOKEN:
                    setTokenCookie(context, value);
                    break;
                default:
                    next.go();
                    break;
            }
        }

        private void setTokenCookie(ApiContext context, String value) throws UnsupportedEncodingException {

            if (config.getAppsDisableTokenCookies().contains(context.appId)) {
                // 屏蔽特定app下的cookie
                return;
            }

            if (value != null && value.length() > 0) {
                String[] tkInfos = value.split("\\|");
                String tk = tkInfos[0];
                int appId = context.appId;
                if(tkInfos.length > 1) {
                    try {
                        appId = Integer.parseInt(tkInfos[1]);
                    } catch (Exception e) {
                        logger.error("parse tk appId error." + value, e);
                    }
                }

                dispatchCookie(context.response, appId + CookieName.token,
                    URLEncoder.encode(tk, StandardCharsets.UTF_8.name()),
                    c -> {
                    c.setMaxAge(3600 * 24 * 7);
                    c.setHttpOnly(true);
                    // c.setDomain(domain);
                });

                // 用于提示客户端当前token是否存在
                dispatchCookie(context.response, appId + CookieName.cookieExist, "1", c -> {
                    c.setMaxAge(3600 * 24 * 7);
                    // c.setDomain(domain);
                });

                context.userTokenExpired = false; // user token will be override.
            } else { // 删除cookie
                context.userTokenExpired = true;
            }
        }

        private void setTTokenCookie(ApiContext apiContext, String value)
            throws UnsupportedEncodingException {

            if (value != null && value.length() > 0) {
                int duration = -1;
                String ttk = value;
                try {
                    int index = value.lastIndexOf("|");
                    if (index > 0) {
                        ttk = value.substring(0, index);
                        duration = Integer.parseInt(value.substring(index + 1));
                    }
                } catch (Exception e) {
                    logger.error("parse ttk expire time error." + value, e);
                }

                int finalDuration = duration;

                dispatchCookie(apiContext.response, apiContext.appId + CookieName.ttoken,
                    URLEncoder.encode(ttk, StandardCharsets.UTF_8.name()), c -> {

                    c.setMaxAge(finalDuration);
                    c.setHttpOnly(true);
                });

                dispatchCookie(apiContext.response,
                    apiContext.appId + CookieName.tempTokenExist,
                    "1",
                    c -> c.setMaxAge(finalDuration));
            }
        }
    }

    @Extension
    class CookieDispatcherImpl implements RequestFinished.CookieDispatcher, CookieSupport {
        @Override
        public void dispatch(ApiContext context) {
            if (!context.userTokenExpired) {
                return;
            }

            GatewayResponse response = context.getResponse();

            // token 解析失败，删除 token 以及标志位
            // 删除 cookie 中的 user token
            dispatchCookie(response, context.appId + CookieName.token, c -> {
                c.setMaxAge(0);
                c.setHttpOnly(true);
            });

            // 删除 cookie 中的 登录标志位
            dispatchCookie(response, context.appId + CookieName.cookieExist, c -> c.setMaxAge(0));
        }
    }
}
