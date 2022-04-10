package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.JSON;
import com.estatetrader.annotation.ApiMockService;
import com.estatetrader.annotation.EnabledMockedApiService;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.phases.parsing.ApiRegister;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.define.MockApiConfigInfo;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.responseEntity.BackendMessageResp;
import com.estatetrader.util.RawString;
import com.estatetrader.algorithm.workflow.ExecutionResult;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.annotation.ApiScribeService;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.gateway.backendmsg.BackendMessageBody;
import com.estatetrader.gateway.backendmsg.PolledBackendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 网关API mock功能
 */
public interface MockServiceFeature {

    @Component
    class Config {
        private boolean featureEnabled = false;

        @Value("${com.estatetrader.apigw.enableMockService:false}")
        public void setFeatureEnabled(boolean featureEnabled) {
            this.featureEnabled = featureEnabled;
        }
    }

    @Extension
    class ApiInfoRegisterImpl implements ApiRegister {
        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            ApiMockService apiMockService = info.proxyMethodInfo.getAnnotation(ApiMockService.class);
            if (apiMockService != null) {
                if (schema.apiMockService != null) {
                    throw new IllegalApiDefinitionException("duplicated @ApiMockService found");
                }
                if (!info.returnType.equals(String.class)) {
                    throw new IllegalApiDefinitionException("the return type of " + info.methodName +
                        " must be of string, since it is annotated by @ApiMockService");
                }
                schema.apiMockService = info;
            }

            EnabledMockedApiService enabledMockedApiService = info.proxyMethodInfo.getAnnotation(EnabledMockedApiService.class);
            if (enabledMockedApiService != null) {
                if (schema.enabledMockedApiService != null) {
                    throw new IllegalApiDefinitionException("duplicated @EnabledMockedApiService found");
                }
                if (!info.returnType.equals(MockApiConfigInfo.class)) {
                    throw new IllegalApiDefinitionException("the return type of " + info.methodName +
                        " must be of MockRelatedApiInfo, since it is annotated by @EnabledMockedApiService");
                }
                schema.enabledMockedApiService = info;
            }

            ApiScribeService apiScribeService = info.proxyMethodInfo.getAnnotation(ApiScribeService.class);
            if (apiScribeService != null) {
                if (schema.apiScribeService != null) {
                    throw new IllegalApiDefinitionException("duplicated @ApiScribeService found");
                }
                schema.apiScribeService = info;
            }
        }
    }

    @Extension(after = SecurityFeature.class)
    class ContextProcessorImpl implements RequestStarted.ContextProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ContextProcessorImpl.class);

        private final Config config;

        public ContextProcessorImpl(Config config) {
            this.config = config;
        }

        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) {
            if (context.mockApiConfigInfo != null
                && !CollectionUtils.isEmpty(context.mockApiConfigInfo.apisToMock)) {
                return; // the final apis to mock has already set by client
            }
            if (!config.featureEnabled) {
                return; // mock service is not enabled
            }

            ApiMethodInfo method = context.apiSchema.enabledMockedApiService;
            if (method == null) {
                return; // no @EnabledMockedApiService defined
            }

            String[] args = new String[method.parameterInfos.length];
            for (int i = 0; i < args.length; i++) {
                String name = method.parameterInfos[i].name;
                if ("parameters".equals(name)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("requestApis", calculateRequestedApisForMockingCheck(context));
                    map.put("deviceId", context.caller != null ? context.caller.deviceId : context.deviceId);
                    map.put("appId", context.appId);
                    map.put("version", context.versionName);
                    map.put("userId", context.caller != null ? context.caller.uid : 0);
                    args[i] = JSON.toJSONString(map);
                }
            }

            context.startApiCall(pipeline, method, args, (result, code) -> {
                if (code == 0) {
                    if (result != null) {
                        MockApiConfigInfo configInfo = (MockApiConfigInfo) result;
                        context.mockApiConfigInfo = configInfo;
                        acceptBackendMessages(context, configInfo);
                    } else {
                        LOGGER.debug("enabledMockedApiService api call result null");
                    }
                } else {
                    LOGGER.warn("enabledMockedApiService api call result error code:{}, req:{}", code, args);
                }
            });
        }

        private void acceptBackendMessages(ApiContext context, MockApiConfigInfo configInfo) {
            if (configInfo.backendMessages == null
                || configInfo.backendMessages.isEmpty()) {
                return;
            }
            synchronized (context.backendMessages) {
                for (BackendMessageResp bmr : configInfo.backendMessages) {
                    BackendMessageBody body = new BackendMessageBody();
                    body.type = bmr.type;
                    body.service = bmr.service;
                    body.content = bmr.content;
                    body.quietPeriod = bmr.quietPeriod;
                    context.backendMessages.add(new PolledBackendMessage(null, body));
                }
            }
        }

        private List<String> calculateRequestedApisForMockingCheck(ApiContext context) {
            ApiSchema schema = context.apiSchema;
            return context.apiCalls
                .stream()
                .filter(c -> c.method != schema.apiMockService
                    && c.method != schema.enabledMockedApiService
                    && c.method != schema.apiScribeService)
                .map(c -> c.method.methodName)
                .collect(Collectors.toList());
        }
    }

    @Extension
    class ApiCallLauncherImpl implements CallStarted.ApiCallLauncher {

        private final Config config;

        public ApiCallLauncherImpl(Config config) {
            this.config = config;
        }

        private boolean mockIsEnabled(ApiMethodCall call, ApiContext context) {
            // return true if we want to intercept this api call, or false to let other launchers have a chance to deal with it
            return config.featureEnabled && // only enabled in prod env
                context.apiSchema.apiMockService != null && // we have an available mock service to use
                call.method.apiMethodType == ApiMethodType.DUBBO && // only support dubbo
                context.mockApiConfigInfo !=null
                && !CollectionUtils.isEmpty(context.mockApiConfigInfo.apisToMock)// client has specified which apis should be mocked
                && context.mockApiConfigInfo.apisToMock.contains(call.method.methodName); // and this api should be mocked
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
            if (!mockIsEnabled(call, context)) {
                return next.go();
            }

            Map<String, Object> parameters = new HashMap<>();
            for (int i = 0; i < call.parameters.length; i++) {
                ApiParameterInfo pi = call.method.parameterInfos[i];
                String text = call.parameters[i];
                Object value;
                if (pi.type.equals(Date.class)) {
                    value = text;
                } else {
                    value = pi.convert(text);
                }
                parameters.put(pi.name, value);
            }

            ApiMethodInfo method = context.apiSchema.apiMockService;
            String[] args = new String[method.parameterInfos.length];
            for (int i = 0; i < method.parameterInfos.length; i++) {
                String name = method.parameterInfos[i].name;
                String value;
                if ("mockedApi".equals(name)) {
                    value = call.method.methodName;
                } else if ("apiParameters".equals(name)) {
                    value = JSON.toJSONString(parameters);
                } else if ("targetDeviceId".equals(name)) {
                    value = String.valueOf(context.caller != null ? context.caller.deviceId : context.deviceId);
                } else if ("targetUserId".equals(name)) {
                    value = String.valueOf(context.caller != null ? context.caller.uid : 0);
                } else {
                    value = null;
                }
                args[i] = value;
            }

            // 如果API被mock，则跳过与之相关的返回值注入逻辑
            call.disableResponseFilters = true;

            ExecutionResult.Async result = new ExecutionResult.Async();

            context.startApiCall(pipeline, context.apiSchema.apiMockService, args, (mock, code, c) -> {
                if (code == 0) {
                    if (mock == null) {
                        result.success(null);
                    } else if (mock instanceof String) {
                        result.success(new RawString((String) mock));
                    } else {
                        result.fail(new GatewayException(
                            ApiReturnCode.ILLEGAL_MOCK_RESULT,
                            new IllegalArgumentException("mock result type " + mock.getClass() + " is not string"))
                        );
                    }
                } else {
                    result.fail(new GatewayException(new ApiReturnCode(c.getReturnMessage(), code)));
                }
            });

            return result;
        }
    }

    @Extension
    class AfterApiCallResultReceivedImpl implements CallResultReceived.AfterApiCallResultReceived {

        private static final Logger LOGGER = LoggerFactory.getLogger(AfterApiCallResultReceivedImpl.class);
        @Override
        public void receive(ApiMethodCall call, ApiContext context, WorkflowPipeline pipeline) throws Exception {
            Object result = call.result;

            if (result == null
                    || context.mockApiConfigInfo ==null
                    || CollectionUtils.isEmpty(context.mockApiConfigInfo.apisToScribe)) {
                return;
            }
            if(!context.mockApiConfigInfo.apisToScribe.contains(call.method.methodName)){
                return;
            }
            Object wrapResult = call.method.responseWrapper.wrap(result);
            String apiResult = JSON.toJSONString(wrapResult);

            Map<String, Object> parameters = new HashMap<>();
            for (int i = 0; i < call.parameters.length; i++) {
                ApiParameterInfo pi = call.method.parameterInfos[i];
                String text = call.parameters[i];
                Object value;
                if (pi.type.equals(Date.class)) {
                    value = text;
                } else {
                    value = pi.convert(text);
                }
                parameters.put(pi.name, value);
            }
            ApiMethodInfo method = context.apiSchema.apiScribeService;
            String[] args = new String[method.parameterInfos.length];
            for (int i = 0; i < method.parameterInfos.length; i++) {
                String name = method.parameterInfos[i].name;
                String value;
                if ("scribeApi".equals(name)) {
                    value = call.method.methodName;
                } else if ("apiParameters".equals(name)) {
                    value = JSON.toJSONString(parameters);
                } else if ("targetDeviceId".equals(name)) {
                    value = String.valueOf(context.caller != null ? context.caller.deviceId : context.deviceId);
                } else if ("targetUserId".equals(name)) {
                    value = String.valueOf(context.caller != null ? context.caller.uid : 0);
                }else if("apiResult".equals(name)){
                    value = apiResult;
                } else {
                    value = null;
                }
                args[i] = value;
            }
            context.startApiCall(pipeline, context.apiSchema.apiScribeService, args, (mock, code, c) -> {
                if (code == 0) {
                    LOGGER.debug("apiScribeService result :{}", JSON.toJSONString(mock));
                } else {
                    LOGGER.warn("apiScribeService error:{}, req:{} ",c.getReturnMessage(), args);
                }
            });
        }
    }
}
