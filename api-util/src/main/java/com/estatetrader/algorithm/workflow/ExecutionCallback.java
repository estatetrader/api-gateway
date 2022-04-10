package com.estatetrader.algorithm.workflow;

/**
 * a callback which will be executed when the execution finished
 */
@FunctionalInterface
public interface ExecutionCallback {
    /**
     * the execution has completed
     * @param result result of the execution if not failed
     * @param throwable the exception thrown when the execution failed
     */
    void onCompleted(Object result, Throwable throwable);
}
