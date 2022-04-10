package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.TypeReference;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.request.RequestFinished;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.services.BackendMessageTopicListener;
import com.estatetrader.apigw.core.support.ApiMDCSupport;
import com.estatetrader.apigw.core.phases.executing.serialize.ResponseSerializer;
import com.estatetrader.apigw.core.utils.AutoTypeSerializer;
import com.estatetrader.define.ConstField;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.gateway.backendmsg.BackendMessageCondition;
import com.estatetrader.util.Lambda;
import com.estatetrader.algorithm.workflow.ExecutionResult;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.gateway.backendmsg.BackendMessageBody;
import com.estatetrader.gateway.backendmsg.PolledBackendMessage;
import com.estatetrader.responseEntity.BackendMessageResp;
import com.estatetrader.responseEntity.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 通用的异步信息返回框架
 *
 * 后台可以在某个任意的时间点生成一条发给某个用户的消息，此消息会在客户端下一次访问网关的时候（调用任意一个API）被带回
 */
public interface BackendMessageFeature {

    Logger LOGGER = LoggerFactory.getLogger(BackendMessageFeature.class);

    // 出于性能考虑，我们选择使用一个独立的执行流中的node来获取用户的后台消息，使之能与API调用并行执行
    @Extension
    class RequestProcessorImpl implements RequestStarted.RequestProcessor, ApiMDCSupport {

        private final BackendMessageTopicListener backendMessageTopicListener;
        private final int pollingLimit;

        public RequestProcessorImpl(@Autowired(required = false) BackendMessageTopicListener backendMessageTopicListener,
                                    @Value("${gateway.backend-messages-polling-limit:100}") int pollingLimit) {
            this.backendMessageTopicListener = backendMessageTopicListener;
            this.pollingLimit = pollingLimit;
        }

        @Override
        public void process(ApiContext context, WorkflowPipeline pipeline) {
            if (context.caller == null ||
                pollingLimit <= 0 /* polling backend messages is disabled */) {
                return;
            }

            if (backendMessageTopicListener != null) {
                pipeline.node("$backend-message", null, pp -> {
                    setupMDC(context);
                    try {
                        Long userId = context.caller.uid != 0 ? context.caller.uid : null;
                        Long deviceId = context.caller.deviceId != 0 ? context.caller.deviceId : null;
                        CompletableFuture<List<PolledBackendMessage>> future =
                            backendMessageTopicListener.poll(userId, deviceId, c -> matchCondition(c, context));
                        return processPolledResult(future, context);
                    } finally {
                        cleanupMDC();
                    }
                });
            }
        }

        private ExecutionResult processPolledResult(CompletableFuture<List<PolledBackendMessage>> future, ApiContext context) {
            if (future == null) {
                return null;
            }
            return ExecutionResult.from(future.handle((list, throwable) -> {
                if (list != null && !list.isEmpty()) {
                    synchronized (context.backendMessages) {
                        context.backendMessages.addAll(list);
                    }
                }
                if (throwable != null) {
                    LOGGER.error("failed to poll backend messages", throwable);
                }
                return null;
            }));
        }

        private boolean matchCondition(BackendMessageCondition con, ApiContext context) {
            LOGGER.debug("match condition {}", con);
            if (con == null) {
                return true;
            }

            if (con.onlyAppIds != null &&
                !con.onlyAppIds.isEmpty() && // 为了兼容不同的编程风格，我们不区分空数组和null
                !con.onlyAppIds.contains(context.appId)) {
                return false;
            }

            if (con.onlyApis != null &&
                !con.onlyApis.isEmpty() && // 为了兼容不同的编程风格，我们不区分空数组和null
                context.apiCalls != null &&
                Lambda.all(context.apiCalls, c -> !con.onlyApis.contains(c.method.methodName))) {
                return false;
            }

            //noinspection RedundantIfStatement
            if (con.ignoredApis != null &&
                !con.ignoredApis.isEmpty() &&
                context.apiCalls != null &&
                Lambda.all(context.apiCalls, c -> con.ignoredApis.contains(c.method.methodName))) {
                return false;
            }

            return true;
        }
    }

    @Extension
    class NotificationProcessorImpl implements CallResultReceived.NotificationProcessor {

        private static final TypeReference<List<BackendMessageBody>> BACKEND_MESSAGE_LIST_TYPE
            = new TypeReference<List<BackendMessageBody>>() {};

        /**
         * 处理来自后端服务器返回的旁路信息
         * 如果你对某个信息不感兴趣，请调用next.go()让其他处理器处理
         *
         * @param name    旁路信息的名称
         * @param value   信息的内容
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param next    调用next.go()让其他旁路信息处理器处理
         */
        @Override
        public void process(String name,
                            String value,
                            ApiContext context,
                            ApiMethodCall call,
                            Next.NoResult<IOException> next) throws IOException {
            if (ConstField.BACKEND_MESSAGE.equals(name)) {
                synchronized (context.backendMessages) {
                    for (BackendMessageBody m : parseList(value)) {
                        context.backendMessages.add(new PolledBackendMessage(null, m));
                    }
                }
            } else {
                next.go();
            }
        }

        private static List<BackendMessageBody> parseList(String value) {
            if (value == null || value.isEmpty()) {
                return Collections.emptyList();
            }

            return AutoTypeSerializer.deserializeFromString("[" + value + "]", NotificationProcessorImpl.BACKEND_MESSAGE_LIST_TYPE);
        }
    }

    @Extension
    class ResponseGeneratorImpl implements RequestFinished.ResponseGenerator {

        private final ResponseSerializer responseSerializer;

        public ResponseGeneratorImpl(Extensions<ResponseSerializer> responseSerializer) {
            this.responseSerializer = responseSerializer.singleton();
        }

        @Override
        public void process(ApiContext context, AbstractReturnCode code, List<ApiMethodCall> calls, Response response) {
            synchronized (context.backendMessages) {
                if(!context.backendMessages.isEmpty()) {
                    response.backendMessages = context.backendMessages.stream()
                        .map(m -> convertBackendMessage(m.body, context))
                        .collect(Collectors.toList());
                }
            }
        }

        private BackendMessageResp convertBackendMessage(BackendMessageBody messageBody, ApiContext context) {
            Serializable content = messageBody.content;
            String contentInString;
            if (content instanceof String) {
                contentInString = (String) content;
            } else {
                contentInString = responseSerializer.toJsonString(content, null, context);
            }
            return new BackendMessageResp(
                contentInString,
                messageBody.type,
                messageBody.service,
                messageBody.quietPeriod
            );
        }
    }
}
