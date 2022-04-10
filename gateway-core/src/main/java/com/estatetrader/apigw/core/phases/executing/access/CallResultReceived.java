package com.estatetrader.apigw.core.phases.executing.access;

import com.alibaba.fastjson.JSON;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.define.ConstField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * API执行阶段
 *
 * 收到API执行的结果，包括分析执行结果，处理旁路信息、调用其他依赖项等
 */
public interface CallResultReceived {

    @Service
    class Execution implements WorkflowExecution.Sync {

        private final Logger logger = LoggerFactory.getLogger(CallResultReceived.class);

        private final Extensions<AfterApiCallResultReceived> afterApiCallResultReceivedList;

        public Execution(Extensions<AfterApiCallResultReceived> afterApiCallResultReceivedList) {
            this.afterApiCallResultReceivedList = afterApiCallResultReceivedList;
        }

        /**
         * whether this execution inherits the failure of its previous stage in the same batch
         *
         * @return return true if you do not want to wrap the failure of previous stage
         */
        @Override
        public boolean inheritPreviousFailure() {
            return true;
        }

        /**
         * execute the execution of the node
         *
         * @param pipeline a pipeline instance which allows you
         *                 to add successors (nodes which depend on this node) of this node
         * @throws Throwable exception occurred while starting this node
         */
        @Override
        public void run(WorkflowPipeline pipeline) throws Throwable {
            ApiContext context = (ApiContext) pipeline.getContext();
            ApiMethodCall call = (ApiMethodCall) pipeline.getParam();

            call.result = pipeline.previousValue();

            if (logger.isDebugEnabled()) {
                logger.debug("api call result received {} = {}",
                    call.method.methodName,
                    JSON.toJSONString(call.result));
            }

            for (AfterApiCallResultReceived h : afterApiCallResultReceivedList) {
                h.receive(call, context, pipeline);
            }
        }
    }

    /**
     * 收到API执行结果之后的处理过程
     */
    interface AfterApiCallResultReceived {
        void receive(ApiMethodCall call, ApiContext context, WorkflowPipeline pipeline) throws Exception;
    }

    @Extension(first = true)
    class AfterApiCallResultReceivedImpl implements AfterApiCallResultReceived {

        final Extensions<NotificationProcessor> notificationProcessors;

        public AfterApiCallResultReceivedImpl(Extensions<NotificationProcessor> notificationProcessors) {
            this.notificationProcessors = notificationProcessors;
        }

        @Override
        public void receive(ApiMethodCall call, ApiContext context, WorkflowPipeline pipeline) throws IOException {
            processIncomingNotifications(call, context, pipeline);
        }

        private void processIncomingNotifications(ApiMethodCall call, ApiContext context, WorkflowPipeline pipeline)
            throws IOException {

            @SuppressWarnings("unchecked")
            Map<String, String> notifications = (Map<String, String>) pipeline.getPreviousData();

            if (notifications == null) {
                return;
            }

            for (Map.Entry<String, String> entry : notifications.entrySet()) {
                notificationProcessors.chain(NotificationProcessor::process,
                    entry.getKey(), entry.getValue(), context, call).go();
            }
        }
    }

    /**
     * 处理API执行时provider端提供的旁路信息
     */
    interface NotificationProcessor {
        /**
         * 处理来自后端服务器返回的旁路信息
         * 如果你对某个信息不感兴趣，请调用next.go()让其他处理器处理
         * @param name 旁路信息的名称
         * @param value 信息的内容
         * @param context 请求上下文
         * @param call 当前请求的API
         * @param next 调用next.go()让其他旁路信息处理器处理
         */
        void process(String name,
                     String value,
                     ApiContext context,
                     ApiMethodCall call,
                     Next.NoResult<IOException> next) throws IOException;
    }

    @Extension(last = true) // NOTE: this class must be the last processor
    class DefaultNotificationProcessor implements NotificationProcessor {

        private final Logger logger = LoggerFactory.getLogger(CallResultReceived.class);

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
                            Next.NoResult<IOException> next) {

            switch (name) {
                case ConstField.SERVICE_LOG:
                    call.serviceLog = value;
                    break;
                case ConstField.REDIRECT_TO:
                    context.response.sendRedirect(value);
                    break;
                default:
                    logger.warn("unrecognized notification found {} = {}", name, value);
            }
        }
    }
}
