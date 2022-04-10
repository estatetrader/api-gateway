package com.estatetrader.apigw.core.contracts;

import com.estatetrader.define.ApiCallCallback;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;

@FunctionalInterface
public interface ApiCallExecutor {

    /**
     * execute an api method call (async)
     * @param call api method call to execute
     * @param args args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    void start(ApiMethodCall call, Object[] args, ApiCallCallback.Complex<Object> callback);

    /**
     * execute an api method call (async)
     * @param methodInfo api method info to execute
     * @param args args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    default void start(ApiMethodInfo methodInfo, Object[] args, ApiCallCallback.Complex<Object> callback) {
        start(new ApiMethodCall(methodInfo), args, callback);
    }

    /**
     * execute an api method call (async)
     * @param call api method call to execute
     * @param args args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    default void start(ApiMethodCall call, Object[] args, ApiCallCallback<Object> callback) {
        start(call, args, (result, code, c) -> callback.onCompleted(result, code));
    }

    /**
     * execute an api method call (async)
     * @param methodInfo api method info to execute
     * @param callback the callback which will be called after api executed
     */
    default void start(ApiMethodInfo methodInfo, ApiCallCallback<Object> callback) {
        start(methodInfo, new Object[methodInfo.parameterInfos.length], callback);
    }

    /**
     * execute an api method call (async)
     * @param methodInfo api method info to execute
     * @param callback the callback which will be called after api executed
     */
    default void start(ApiMethodInfo methodInfo, ApiCallCallback.Simple<Object> callback) {
        start(methodInfo, new Object[methodInfo.parameterInfos.length], callback);
    }

    /**
     * execute an api method call (async)
     * @param methodInfo api method info to execute
     * @param args args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    default void start(ApiMethodInfo methodInfo, Object[] args, ApiCallCallback<Object> callback) {
        start(new ApiMethodCall(methodInfo), args, callback);
    }

    /**
     * execute an api method call (async)
     * @param methodInfo api method info to execute
     * @param args args used to execute that api
     * @param callback the callback which will be called after api executed
     */
    default void start(ApiMethodInfo methodInfo, Object[] args, ApiCallCallback.Simple<Object> callback) {
        start(new ApiMethodCall(methodInfo), args, callback);
    }
}
