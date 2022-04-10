package com.estatetrader.apigw.core.models;

import com.alibaba.fastjson.JSONObject;
import com.estatetrader.apigw.core.contracts.ApiCallExecutor;
import com.estatetrader.define.ApiCallCallback;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;

public class ApiCallExecutorImpl implements ApiCallExecutor {

    public final WorkflowPipeline pipeline;
    public final WorkflowExecution[] executeApiCall;

    public ApiCallExecutorImpl(WorkflowPipeline pipeline, WorkflowExecution[] executeApiCall) {
        this.pipeline = pipeline;
        this.executeApiCall = executeApiCall;
    }

    public ApiCallExecutorImpl withPipeline(WorkflowPipeline pipeline) {
        return new ApiCallExecutorImpl(pipeline, executeApiCall);
    }

    /**
     * execute an api method call (async)
     *
     * @param call     api method call to execute
     * @param args     args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    @Override
    public void start(ApiMethodCall call, Object[] args, ApiCallCallback.Complex<Object> callback) {
        call.parameters = new String[call.method.parameterInfos.length];

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                Object arg = args[i];
                if (arg instanceof String) {
                    call.parameters[i] = (String) arg;
                } else if (arg != null) {
                    call.parameters[i] = JSONObject.toJSONString(arg);
                }
            }
        }

        WorkflowExecution.Sync stage = p -> {
            ApiMethodCall c = (ApiMethodCall) p.getParam();
            callback.onCompleted(c.result, c.getReturnCode(), c);
        };
        pipeline.stage(call, executeApiCall, stage);
    }
}
