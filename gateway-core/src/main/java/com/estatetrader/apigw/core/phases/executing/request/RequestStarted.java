package com.estatetrader.apigw.core.phases.executing.request;

import com.estatetrader.apigw.core.contracts.GatewayCookie;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import com.estatetrader.core.ApiNotFoundException;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.util.Lambda;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.support.ApiMDCSupport;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 请求执行阶段
 *
 * 开始处理请求，包括解析、校验和执行请求等
 */
public interface RequestStarted {

    @Service
    class Execution implements WorkflowExecution.Sync, ApiMDCSupport {

        private final WorkflowExecution[] steps;

        public Execution(ParseRequestExecution parseRequest,
                         ProcessContextExecution processContext,
                         VerifyRequestExecution verifyRequest,
                         ProcessRequestExecution processRequest) {

            this.steps = new WorkflowExecution[] {
                parseRequest,
                processContext,
                verifyRequest,
                processRequest
            };
        }

        /**
         * execute the execution of the node
         *
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node)
         *                 of this node
         */
        @Override
        public void run(WorkflowPipeline pipeline) {
            ApiContext context = (ApiContext) pipeline.getContext();
            context.startTime = System.currentTimeMillis();
            pipeline.stage(steps);
        }
    }

    @Service
    class ParseRequestExecution implements WorkflowExecution.Sync, ApiMDCSupport {

        private final Extensions<RequestParser> requestParsers;

        public ParseRequestExecution(Extensions<RequestParser> requestParsers) {
            this.requestParsers = requestParsers;
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
                for (RequestParser p : requestParsers) {
                    p.parse(context);
                }
            } finally {
                cleanupMDC();
            }
        }
    }

    @Service
    class ProcessContextExecution implements WorkflowExecution.Sync, ApiMDCSupport {

        private final Extensions<ContextProcessor> contextProcessors;

        public ProcessContextExecution(Extensions<ContextProcessor> contextProcessors) {
            this.contextProcessors = contextProcessors;
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
                for (ContextProcessor p : contextProcessors) {
                    p.process(context, pipeline);
                }
            } finally {
                cleanupMDC();
            }
        }
    }

    @Service
    class VerifyRequestExecution implements WorkflowExecution.Sync, ApiMDCSupport {

        private final Extensions<RequestVerifier> requestVerifiers;

        public VerifyRequestExecution(Extensions<RequestVerifier> requestVerifiers) {
            this.requestVerifiers = requestVerifiers;
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
                for (RequestVerifier v : requestVerifiers) {
                    v.verify(context);
                }
            } finally {
                cleanupMDC();
            }
        }
    }

    @Service
    class ProcessRequestExecution implements WorkflowExecution.Sync, ApiMDCSupport {

        private final Extensions<RequestProcessor> requestProcessors;

        public ProcessRequestExecution(Extensions<RequestProcessor> requestProcessors) {
            this.requestProcessors = requestProcessors;
        }

        /**
         * execute the execution of the node
         *
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
         */
        @Override
        public void run(WorkflowPipeline pipeline) {
            ApiContext context = (ApiContext) pipeline.getContext();
            setupMDC(context);
            try {
                for (RequestProcessor p : requestProcessors) {
                    p.process(context, pipeline);
                }
            } finally {
                cleanupMDC();
            }
        }
    }

    /**
     * 解析请求信息，并将解析结果写入到ApiContext中
     */
    interface RequestParser {
        void parse(ApiContext context) throws GatewayException;
    }

    /**
     * 对ApiContext的信息进行进一步处理
     */
    interface ContextProcessor {
        void process(ApiContext context, WorkflowPipeline pipeline) throws GatewayException;
    }

    /**
     * 对请求进行合法性校验，大多数request级别的错误码都应在此抛出
     */
    interface RequestVerifier {
        void verify(ApiContext context) throws GatewayException;
    }

    /**
     * 请求处理器
     */
    interface RequestProcessor {
        void process(ApiContext context, WorkflowPipeline pipeline);
    }

    @Extension(first = true)
    class RequestParserImpl implements RequestParser {

        private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

        private final Logger logger = LoggerFactory.getLogger(RequestStarted.class);
        private final Extensions<HeaderParser> headerParsers;
        private final Extensions<ParameterParser> parameterParsers;
        private final Extensions<CookieParser> cookieParsers;

        public RequestParserImpl(Extensions<HeaderParser> headerParsers,
                                 Extensions<ParameterParser> parameterParsers,
                                 Extensions<CookieParser> cookieParsers) {
            this.headerParsers = headerParsers;
            this.parameterParsers = parameterParsers;
            this.cookieParsers = cookieParsers;
        }

        @Override
        public void parse(ApiContext context) throws GatewayException {

            // 解析请求头
            for (HeaderParser p : headerParsers) {
                p.parse(context);
            }

            setContentType(context);

            // 构造请求字符串用于日志记录
            parseRequestInfo(context);

            // 解析参数
            for (ParameterParser p : parameterParsers) {
                p.parse(context);
            }

            // 解析cookie
            for (CookieParser p : cookieParsers) {
                p.parse(context);
            }

            parseMethod(context);

            for (int methodIndex = 0; methodIndex < context.apiCalls.size(); methodIndex++) {
                parseMethodInfo(context, methodIndex);
            }
        }

        private void parseMethod(ApiContext context) throws GatewayException {
            context.method = context.getRequest().getParameter(CommonParameter.method);
            parseMethodDependency(context);
        }

        private void parseRequestInfo(ApiContext context) {

            Map<String, String> requestInfo = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : context.request.getParameters().entrySet()) {
                List<String> values = entry.getValue().stream().distinct().collect(Collectors.toList());
                if (values.size() > 1) {
                    String text = StringUtils.join(values, "|||");
                    logger.warn("parameter {} has {} values {}", entry.getKey(), values.size(), text);
                }
                requestInfo.put(entry.getKey(),  values.size() == 0 ? "" : values.get(0));
            }

            context.requestInfo = requestInfo;
        }

        private void setContentType(ApiContext context) {
            context.getResponse().setContentType(CONTENT_TYPE_JSON);
        }

        private void parseMethodDependency(ApiContext context) throws GatewayException {
            if (context.method == null || context.method.isEmpty()) {
                throw new GatewayException(ApiReturnCode.REQUEST_PARSE_ERROR);
            }

            // 解析多个由','拼接的api名. api名由3个部分组成
            // 函数名@实例名:依赖函数名1@实例名/依赖函数名2@实例名  除了函数名以外的信息都可以缺省.
            String[] names = context.method.split(",");
            List<ApiMethodCall> apiCallList = context.apiCalls = new ArrayList<>(names.length);
            Map<String, ApiMethodCall> apiCallMap = new HashMap<>(names.length);

            for (String fullName : names) {
                String instanceName = fullName.contains(":") ?
                    fullName.substring(0, fullName.indexOf(':')) : fullName;

                String name = instanceName.contains("@") ?
                    instanceName.substring(0, instanceName.indexOf('@')) : instanceName;
                try {
                    ApiMethodInfo method = context.apiSchema.getApiInfo(name);
                    ApiMethodCall call = new ApiMethodCall(method);
                    call.fromClient = true;
                    apiCallList.add(call);
                    apiCallMap.put(instanceName, call);
                } catch (ApiNotFoundException e) {
                    AbstractReturnCode code = new ApiReturnCode(
                        "API " + name + " is not defined",
                        ApiReturnCode._C_UNKNOWN_METHOD);

                    throw new GatewayException(code);
                }
            }

            // 遍历请求中的所有接口, 生成依赖关系
            for (int m = 0; m < names.length; m++) {
                String fullName = names[m];
                ApiMethodCall call = apiCallList.get(m);

                String[] dependentMethods = fullName.contains(":") ?
                    fullName.substring(fullName.indexOf(':') + 1).split("/") : null;

                if (dependentMethods != null) {
                    for (String methodName : dependentMethods) {
                        ApiMethodCall c = apiCallMap.get(methodName);
                        if (c == null) {
                            throw new GatewayException(ApiReturnCode.UNKNOWN_DEPENDENT_METHOD);
                        }
                        call.depend(c);
                    }
                }
            }
        }

        private void parseMethodInfo(ApiContext context, int methodIndex) {

            ApiMethodCall call = context.apiCalls.get(methodIndex);

            // 解析业务参数使其对应各自业务api
            parseMethodParameters(context, methodIndex);

            if (context.apiCalls.size() == 1) {
                call.businessId = context.request.getParameter(CommonParameter.businessId);
            } else {
                call.businessId = context.request.getParameter(methodIndex + "_" + CommonParameter.businessId);
            }
        }

        private void parseMethodParameters(ApiContext context, int methodIndex) {

            ApiMethodCall call = context.apiCalls.get(methodIndex);
            ApiMethodInfo method = call.method;

            call.parameters = new String[method.parameterInfos.length];

            for (int i = 0; i < call.parameters.length; i++) {
                ApiParameterInfo ap = method.parameterInfos[i];
                if (ap.serverOnly) {
                    continue;
                }

                if (context.apiCalls.size() == 1) {
                    call.parameters[i] = context.request.getParameter(ap.name);
                } else {
                    String name = methodIndex + "_" + ap.name;
                    call.parameters[i] = context.request.getParameter(name);
                }
            }
        }
    }

    /**
     * 请求解析之请求头解析
     */
    interface HeaderParser {
        void parse(ApiContext context) throws GatewayException;
    }

    @Extension(first = true)
    class HeaderParserImpl implements HeaderParser {

        private static final String X_UNIQUE_ID = "X-Unique-ID";
        private static final String SERVER_ADDRESS = "a:";
        private static final String THREAD_ID = "t:";
        private static final String SPLIT = "|";
        private static final String REQ_TAG = "s:";

        private static final String X_FORWARDED_PROTO    = "x-forwarded-proto";
        private static final String X_FORWARDED_FOR      = "x-forwarded-for";
        private static final String HOST_HEADER_NAME     = "host";
        private static final String X_FORWARDED_HOST     = "x-forwarded-host";
        private static final String HTTP_X_FORWARDED_FOR = "http-x-forwarded-for";
        private static final String REMOTE_ADDR          = "remote-addr";

        private final String hostname = determineHostname();

        @Override
        public void parse(ApiContext context) {
            parseCallId(context);
            parseClientIP(context);
            context.httpsMode = "https".equals(context.getRequest().getHeader(X_FORWARDED_PROTO));
            context.host = context.request.getHeader(X_FORWARDED_HOST);
            if (context.host == null) {
                context.host = context.request.getHeader(HOST_HEADER_NAME);
            }
        }

        private void parseCallId(ApiContext context) {
            String cid = context.request.getHeader(X_UNIQUE_ID);
            if (cid == null || cid.isEmpty()) {
                cid = SERVER_ADDRESS + hostname
                    + SPLIT + THREAD_ID + Thread.currentThread().getId()
                    + SPLIT + REQ_TAG + context.startTime;
            }
            context.cid = cid;
            MDC.put(CommonParameter.callId, cid);
        }

        private void parseClientIP(ApiContext context) {
            List<String> clientIPList = getClientIPList(context.request);
            String clientIP;

            if (clientIPList.isEmpty()) {
                clientIP = null;
            } else {
                clientIP = clientIPList.get(0);
            }

            context.clientIP = clientIP;
            MDC.put(CommonParameter.clientIp, clientIP);
        }

        private List<String> getClientIPList(GatewayRequest request) {
            String ipList = Lambda.cascade(
                request.getHeader(X_FORWARDED_FOR),
                request.getHeader(HTTP_X_FORWARDED_FOR),
                request.getHeader(REMOTE_ADDR),
                request.getRemoteAddr());

            if (ipList == null) {
                return Collections.emptyList();
            }

            return Lambda.filter(ipList.split(" *, *"), s -> !s.isEmpty());
        }

        private static String determineHostname() {
            try {
                String name = InetAddress.getLocalHost().getHostName();
                return name != null && name.length() > 6 ? name.substring(0, 6) : name;
            } catch (UnknownHostException e) {
                return "localhost";
            }
        }
    }

    /**
     * 请求解析之参数解析
     */
    interface ParameterParser {
        void parse(ApiContext context) throws GatewayException;
    }

    @Extension(first = true)
    class ParameterParserImpl implements ParameterParser {

        @Override
        public void parse(ApiContext context) {
            GatewayRequest request = context.getRequest();

            context.agent = request.getHeader(HttpHeaders.USER_AGENT);
            context.referer = request.getParameter(CommonParameter.referer);
            if (context.referer == null || context.referer.isEmpty()) {
                context.referer = request.getHeader(HttpHeaders.REFERER);
            }

            String appId = request.getParameter(CommonParameter.applicationId);
            context.appId = (appId != null && appId.length() != 0) ? Integer.parseInt(appId) : 0;
            MDC.put(CommonParameter.applicationId, String.valueOf(context.appId));

            context.versionCode = request.getParameter(CommonParameter.versionCode);
            context.versionName = request.getParameter(CommonParameter.versionName);
        }
    }

    /**
     * 请求解析之cookie解析
     */
    interface CookieParser {
        void parse(ApiContext context);
    }

    @Extension
    class CookieParserImpl implements CookieParser {
        @Override
        public void parse(ApiContext context) {
            for (GatewayCookie cookie : context.request.getCookies()) {
                context.addCookie(cookie.name(), cookie.value());
            }

            // 优先使用url中覆写的 cookie 值
            String cookies = context.getRequest().getParameter(CommonParameter.cookie);
            if (cookies != null && cookies.length() > 0) {
                String[] cos = cookies.split("&");
                for (String c : cos) {
                    int index = c.indexOf('=');
                    if (index > 0 && index != c.length()) {
                        context.addCookie(c.substring(0, index).trim(), c.substring(index + 1));
                    }
                }
            }
        }
    }

    @Extension
    class RequestProcessorImpl implements RequestProcessor {
        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) {
            createAllApiCallNodes(context, pipeline);
        }

        private void createAllApiCallNodes(ApiContext context, WorkflowPipeline pipeline) {
            ApiContext apiContext = (ApiContext) pipeline.getContext();
            for (ApiMethodCall call : apiContext.apiCalls) {
                if (!pipeline.containsNode(call.executionId)) {
                    createCallNode(call, context, pipeline);
                }
            }
        }

        private void createCallNode(ApiMethodCall call, ApiContext context, WorkflowPipeline pipeline) {
            if (call.prev == null) {
                pipeline.node(call.executionId, call, context.executeApiCall);
            } else {
                for (ApiMethodCall prev : call.prev) {
                    if (!pipeline.containsNode(prev.executionId)) {
                        createCallNode(prev, context, pipeline);
                    }
                }
                Iterable<String> deps = Lambda.map(call.prev, c -> c.executionId);
                pipeline.node(call.executionId, deps, call, context.executeApiCall);
            }
        }
    }
}
