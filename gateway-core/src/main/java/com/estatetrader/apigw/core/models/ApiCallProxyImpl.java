package com.estatetrader.apigw.core.models;

import com.estatetrader.define.ApiCallCallback;
import com.estatetrader.define.ApiCallProxy;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;

import java.util.Set;
import java.util.function.Function;

/**
 * add a parameter of this type to your parameter list in the mix function, if you want to execute some APIs on the fly.
 */
public class ApiCallProxyImpl implements ApiCallProxy {

    private final ApiMethodInfo methodInfo;
    private final ApiCallExecutorImpl executor;
    private final Set<String> optional;

    public ApiCallProxyImpl(ApiMethodInfo methodInfo, ApiCallExecutorImpl executor, Set<String> optional) {
        this.methodInfo = methodInfo;
        this.executor = executor;
        this.optional = optional;
    }

    /**
     * optional list
     *
     * @return optional list
     */
    @Override
    public Set<String> optional() {
        return optional;
    }

    /**
     * get an instance of a certain type
     *
     * @param serviceType service type
     * @return instance of the service
     */
    @Override
    public <T> ApiCallService<T> service(Class<T> serviceType) {
        if (methodInfo.servicesAllowedToCall == null) {
            throw new IllegalArgumentException("no service is allowed to execute");
        }
        ServiceProxy serviceProxy = methodInfo.servicesAllowedToCall.get(serviceType);
        if (serviceProxy == null) {
            throw new IllegalArgumentException("service of type " + serviceType + " is not allowed to execute");
        }

        return new ApiCallServiceImpl<>(serviceProxy);
    }

    /**
     * series of blocks which will be executed in order
     *
     * @param blocks blocks to execute
     */
    @Override
    public void sequence(SequenceBlock... blocks) {
        WorkflowExecution.Sync[] executions = new WorkflowExecution.Sync[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            SequenceBlock block = blocks[i];
            executions[i] = p -> block.run(withPipeline(p));
        }
        executor.pipeline.stage(executions);
    }

    public ApiCallProxyImpl withPipeline(WorkflowPipeline pipeline) {
        return new ApiCallProxyImpl(methodInfo, executor.withPipeline(pipeline), optional);
    }

    private class ApiCallServiceImpl<T> implements ApiCallService<T> {

        final ServiceProxy serviceProxy;

        ApiCallServiceImpl(ServiceProxy serviceProxy) {
            this.serviceProxy = serviceProxy;
        }

        /**
         * call an api
         *
         * @param calling  which api you want to call and its parameters to pass
         * @param callback what to do after we got the api result
         */
        @SuppressWarnings("unchecked")
        @Override
        public <R> ApiCallService<T> api(Function<T, R> calling, ApiCallCallback<R> callback) {
            calling.apply((T) serviceProxy.getProxy(executor, (ApiCallCallback<Object>) callback));
            return this;
        }
    }
}
