package com.estatetrader.apigw.core.phases.executing.access;

import com.alibaba.fastjson.JSON;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.models.ApiMethodType;
import com.estatetrader.apigw.core.support.ApiMDCSupport;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.CookieName;
import com.estatetrader.define.SecurityType;
import com.estatetrader.dubboext.DubboExtProperty;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcContext;
import com.estatetrader.algorithm.workflow.ExecutionResult;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.Constants.*;

/**
 * API执行阶段
 *
 * 开始执行API，包括准备API需要的参数（业务参数和自动注入），根据API类型调用相应的执行器启动API
 */
public interface CallStarted {

    @Service
    class Execution implements WorkflowExecution {

        private final Extensions<BeforeApiExecuted> beforeApiExecutedList;
        private final Extensions<ApiCallLauncher> apiCallLaunchers;

        public Execution(Extensions<BeforeApiExecuted> beforeApiExecutedList,
                         Extensions<ApiCallLauncher> apiCallLaunchers) {
            this.beforeApiExecutedList = beforeApiExecutedList;
            this.apiCallLaunchers = apiCallLaunchers;
        }

        /**
         * start the execution of the node
         *
         * @param pipeline a pipeline instance which allows you
         *                to add successors (nodes which depend on this node) of this node
         * @throws Throwable exception occurred while starting this node
         */
        @Override
        public ExecutionResult start(WorkflowPipeline pipeline) throws Throwable {
            ApiContext context = (ApiContext) pipeline.getContext();
            ApiMethodCall call = (ApiMethodCall) pipeline.getParam();

            for (BeforeApiExecuted h : beforeApiExecutedList) {
                h.process(call, context);
            }

            call.startTime = System.currentTimeMillis();

            return apiCallLaunchers.chain(ApiCallLauncher::launch, call, context, pipeline).go();
        }
    }

    /**
     * 在API开始执行之前，对上下文call进行相关初始化工作
     */
    interface BeforeApiExecuted {
        void process(ApiMethodCall call, ApiContext context) throws GatewayException;
    }

    @Extension(first = true)
    class BeforeApiExecutedImpl implements BeforeApiExecuted {
        static final String TRUNCATED_PARAM_MARK = "...truncated!";

        private final Extensions<ParameterValueProvider> parameterValueProviders;
        private final int fieldMaxBytesForLog;

        public BeforeApiExecutedImpl(Extensions<ParameterValueProvider> parameterValueProviders,
                                     @Value("${com.estatetrader.field.max.bytes.for.log:2097152}") int fieldMaxBytesForLog) {
            this.parameterValueProviders = parameterValueProviders;
            this.fieldMaxBytesForLog = fieldMaxBytesForLog;
        }

        @Override
        public void process(ApiMethodCall call, ApiContext context) throws GatewayException {
            String[] parameters = call.parameters;

            for (int i = 0; i < call.parameters.length; i++) {
                for (ParameterValueProvider h : parameterValueProviders) {
                    parameters[i] = h.provide(call, call.method.parameterInfos[i], parameters[i], context);
                }
            }

            updateParameterMessage(call, context);
            setupRpcContextForExecute(call, context);
        }

        private void updateParameterMessage(ApiMethodCall call, ApiContext context) {
            for (int i = 0; i < call.parameters.length; i++) {
                ApiParameterInfo ap = call.method.parameterInfos[i];
                if (ap.ignoreForSecurity) {
                    context.ignoreParameterForSecurity(ap.name);
                } else {
                    String value = trimParameterForLog(call.parameters[i]);
                    call.message.append(ap.name).append('=').append(value).append('&');
                }
            }
            if (call.message.length() > 0 && call.message.charAt(call.message.length() - 1) == '&') {
                call.message.deleteCharAt(call.message.length() - 1);
            }
        }

        private String trimParameterForLog(String param) {
            if (param == null) {
                return null;
            }

            if (param.length() <= fieldMaxBytesForLog) {
                return param;
            }

            return param.substring(0, fieldMaxBytesForLog - TRUNCATED_PARAM_MARK.length()) + TRUNCATED_PARAM_MARK;
        }

        private void setupRpcContextForExecute(ApiMethodCall call, ApiContext apiContext) {
            RpcContext ctx = RpcContext.getContext();

            ctx.setAttachment(CommonParameter.callId, apiContext.cid);
            ctx.setAttachment(CommonParameter.method, call.method.methodName);
            ctx.setAttachment(CommonParameter.clientIp, apiContext.clientIP);
            ctx.setAttachment(CommonParameter.versionName, apiContext.versionName);
            ctx.setAttachment(CookieName.deviceId, String.valueOf(apiContext.deviceId));

            ctx.setAttachment(HttpHeaders.REFERER, apiContext.referer != null ?
                apiContext.referer.length() < 1024 ? apiContext.referer : apiContext.referer.substring(0, 1024) : null);

            ctx.setAttachment(CommonParameter.businessId,
                (call.businessId != null && call.businessId.length() < 4096) ? call.businessId : null);
            ctx.setAttachment(CommonParameter.applicationId, String.valueOf(apiContext.appId));

            if (apiContext.caller == null) {
                ctx.setAttachment(CommonParameter.deviceId, null);
                ctx.setAttachment(CommonParameter.userId, null);
            } else {
                ctx.setAttachment(CommonParameter.deviceId,
                    apiContext.caller.deviceId != 0 ? String.valueOf(apiContext.caller.deviceId) : null);
                ctx.setAttachment(CommonParameter.userId,
                    apiContext.caller.uid != 0 ? String.valueOf(apiContext.caller.uid) : null);
            }
        }
    }

    /**
     * 为API执行提供所需的参数值
     */
    interface ParameterValueProvider {
        String provide(ApiMethodCall call, ApiParameterInfo info, String value, ApiContext context)
            throws GatewayException;
    }

    @Extension(first = true)
    class ParameterValueProviderImpl implements ParameterValueProvider {

        private final Extensions<AutowiredParameterValueProvider> autowiredParameterValueProviders;

        public ParameterValueProviderImpl(Extensions<AutowiredParameterValueProvider> autowiredParameterValueProviders) {
            this.autowiredParameterValueProviders = autowiredParameterValueProviders;
        }

        @Override
        public String provide(ApiMethodCall call,
                              ApiParameterInfo info,
                              String value, ApiContext context) throws GatewayException {
            if (info.isAutowired) {
                return autowiredParameterValueProviders.chain(
                    AutowiredParameterValueProvider::autowire,
                    call, info, context).go(value);
            } else {
                return value;
            }
        }
    }

    /**
     * 为自动注入的参数提供参数值
     */
    interface AutowiredParameterValueProvider {
        /**
         * 注入可注入的参数
         *
         * 根据call和info判断你是否能够为当前参数提供注入，并将最终注入结果作为返回值返回出去
         * 如果你不能为当前参数提供注入，则调用next.go()将机会留给其他注入器
         *
         * @param call 当前待注入的API call
         * @param info 当前待注入的参数信息
         * @param context 请求上下文
         * @param next 如果你需要其他注入器提供注入，则调用next.go()
         * @return 参数最终要注入的值
         * @throws GatewayException 抛出错误码
         */
        String autowire(ApiMethodCall call,
                        ApiParameterInfo info,
                        ApiContext context,
                        Next<String, GatewayException> next) throws GatewayException;
    }

    @Extension(first = true)
    class AutowiredParameterValueProviderImpl implements AutowiredParameterValueProvider {

        private final Logger logger = LoggerFactory.getLogger(CallStarted.class);

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
                case CommonParameter.userAgent:
                    return context.agent;
                case CommonParameter.applicationId:
                    return String.valueOf(context.appId);
                case CommonParameter.clientIp:
                    return context.clientIP;
                case CommonParameter.versionCode:
                    return context.versionCode;
                case CommonParameter.versionName:
                    return context.versionName;
                case CommonParameter.host:
                    return context.host;
                case CommonParameter.callId:
                    return context.cid;
                case CommonParameter.postBody:
                    return SecurityType.Integrated.check(call.method.securityLevel) ? readPostBody(context.request) : null;
                case CommonParameter.referer:
                    return context.referer;
                default:
                    return next.go();
            }
        }

        private String readPostBody(GatewayRequest request) {
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
            } catch (Exception e) {
                logger.error("read post body failed.", e);
            }
            return sb.toString();
        }
    }

    @Extension(last = true) // all parameters that is not autowired by others
    class DefaultAutowiredParameterValueProviderImpl implements AutowiredParameterValueProvider {

        final Logger logger = LoggerFactory.getLogger(CallStarted.class);

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
         */
        @Override
        public String autowire(ApiMethodCall call,
                               ApiParameterInfo info,
                               ApiContext context,
                               Next<String, GatewayException> next) {
            logger.warn("invalid autowired parameter {}", info.name);
            String newValue = context.request.getParameter(info.name);
            return newValue != null ? newValue : next.previousResult();
        }
    }

    /**
     * API启动器
     */
    interface ApiCallLauncher {
        /**
         * 启动API的执行过程
         * @param call 需要执行的API call
         * @param context 请求上下文
         * @param pipeline 用于异步调度的pipeline对象
         * @param next 如果当前launcher无法启动该API，则调用next.go()让后续启动器去启动
         * @return 异步启动的执行结果，可以包含异步状态
         * @throws GatewayException 需要抛出的错误码
         */
        ExecutionResult launch(ApiMethodCall call,
                               ApiContext context,
                               WorkflowPipeline pipeline,
                               Next<ExecutionResult, GatewayException> next) throws GatewayException;
    }

    @Extension(last = true) // 设置为最后一个启动器，让其他启动器有机会尝试
    class ApiCallLauncherImpl implements ApiCallLauncher, ApiMDCSupport {

        private final Logger logger = LoggerFactory.getLogger(CallStarted.class);
        private final Field appResponseFuture;
        private final Set<String> internalAttachmentKeys = new HashSet<>(Arrays.asList(
            PATH_KEY,
            INTERFACE_KEY,
            GROUP_KEY,
            VERSION_KEY,
            DUBBO_VERSION_KEY,
            TOKEN_KEY,
            TIMEOUT_KEY,
            ASYNC_KEY,
            TAG_KEY,
            FORCE_USE_TAG
        ));

        public ApiCallLauncherImpl() {
            try {
                appResponseFuture = FutureAdapter.class.getDeclaredField("appResponseFuture");
                appResponseFuture.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("unsupported dubbo version", e);
            }
        }

        /**
         * 启动API的执行过程
         *
         * @param call     需要执行的API call
         * @param context  请求上下文
         * @param pipeline 用于异步调度的pipeline对象
         * @param next     如果当前launcher无法启动该API，则调用next.go()让后续启动器去启动
         * @return 异步启动的执行结果，可以包含异步状态
         * @throws GatewayException 需要抛出的错误码
         */
        @Override
        public ExecutionResult launch(ApiMethodCall call,
                                      ApiContext context,
                                      WorkflowPipeline pipeline,
                                      Next<ExecutionResult, GatewayException> next) throws GatewayException {
            // 当接口声明了静态 mock 返回值或被标记为短路时
            if (call.method.staticMockValue != null) {
                return ExecutionResult.success(call.method.staticMockValue);
            }

            if (call.method.apiMethodType == ApiMethodType.DUBBO) {
                return startDubboApi(call, context, pipeline);
            }

            throw new IllegalStateException("could not find any api call launcher to execute the call: " +
                call.method.apiMethodType);
        }

        private ExecutionResult startDubboApi(ApiMethodCall call, ApiContext context, WorkflowPipeline pipeline) throws GatewayException {

            RpcContext rpcContext = RpcContext.getContext();
            // dubbo 在调用结束后不会清除 Future 为了避免拿到之前接口对应的 Future 在这里统一清除
            rpcContext.setFuture(null);

            // 准备参数
            Object[] args = new Object[call.method.parameterInfos.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = call.method.parameterInfos[i].convert(call.parameters[i]);
            }

            DubboExtProperty.clearNotifications();

            // 通过服务代理来执行目标函数
            Object value;
            try {
                value = call.method.proxyMethodInfo.invoke(call.method.serviceInstance.get(), args);
            } catch (InvocationTargetException e) {
                return ExecutionResult.fail(e.getTargetException());
            } catch (Exception e) {
                return ExecutionResult.fail(e);
            }

            // 获取本地产生的notifications
            Map<String, String> notifications = new HashMap<>(DubboExtProperty.getCurrentNotifications());
            DubboExtProperty.clearNotifications();

            FutureAdapter<?> future = (FutureAdapter<?>) rpcContext.getCompletableFuture();
            if (future == null) {
                pipeline.setData(notifications);
                return ExecutionResult.success(value);
            }

            ExecutionResult.Async result = new ExecutionResult.Async();
            getAppResponseFuture(future).handle((appResponse, t) -> {
                setupMDC(context, call.method);
                try {
                    if (t != null) {
                        result.fail(t instanceof CompletionException ? t.getCause() : t);
                        return null;
                    }

                    Map<String, String> remoteNotifications = appResponse.getAttachments();
                    if (remoteNotifications != null) {
                        for (Map.Entry<String, String> entry : remoteNotifications.entrySet()) {
                            if (!internalAttachmentKeys.contains(entry.getKey())) {
                                notifications.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("notifications received: {}", JSON.toJSONString(notifications));
                    }

                    pipeline.setData(notifications);
                    result.complete(appResponse.getValue(), appResponse.getException());
                    return null;
                } finally {
                    cleanupMDC();
                }
            });

            return result;
        }

        private CompletableFuture<AppResponse> getAppResponseFuture(FutureAdapter<?> future) {
            try {
                //noinspection unchecked
                return (CompletableFuture<AppResponse>) appResponseFuture.get(future);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("unsupported dubbo version");
            }
        }
    }
}
