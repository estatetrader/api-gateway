package com.estatetrader.algorithm.workflow;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * the result returned by the workflow execution
 * from this interface, we could set a callback which will be called when the execution completed
 */
@FunctionalInterface
public interface ExecutionResult {
    /**
     * set the callback of the execution. the implementing method must ensure that whenever this set callback is called
     * that callback must be called, even if the execution has already completed.
     * @param callback execution callback
     */
    void setCallback(ExecutionCallback callback);

    /**
     * an async mode execution result
     */
    class Async implements ExecutionResult {

        private boolean completed;
        private ExecutionCallback callback;
        private Object result;
        private Throwable throwable;

        /**
         * create an async mode execution result instance, when you actually finished your execution ,please call Async.complete to finish its async
         */
        public Async() {}

        /**
         * set the callback of the execution. the implementing method must ensure that whenever this set callback is called
         * that callback must be called, even if the execution has already completed.
         *
         * @param callback execution callback
         */
        @Override
        public void setCallback(ExecutionCallback callback) {
            boolean completed;
            synchronized (this) {
                if (this.callback != null) {
                    throw new IllegalStateException("callback has already been set");
                }
                completed = this.completed;
                this.callback = callback;
            }
            if (completed) {
                // after completed, the result/throwable will not be updated anymore
                callback.onCompleted(result, throwable);
            }
        }
        /**
         * the execution has completed (succeeded or failed)
         * @param result the result of the execution (if succeeded)
         * @param throwable the exception thrown while executing (if failed)
         */
        public void complete(Object result, Throwable throwable) {
            ExecutionCallback callback;
            synchronized (this) {
                if (this.completed) {
                    return; // duplicate call
                }
                this.completed = true;
                callback = this.callback;
                this.result = result;
                this.throwable = throwable;
            }
            if (callback != null) {
                callback.onCompleted(result, throwable);
            }
        }

        /**
         * execution has succeeded
         * @param result result of the execution
         */
        public void success(Object result) {
            complete(result, null);
        }

        /**
         * the execution has failed
         * @param throwable exception thrown while executing
         */
        public void fail(Throwable throwable) {
            if (throwable == null) {
                throw new NullPointerException("throwable cannot be null");
            }
            complete(null, throwable);
        }
    }

    /**
     * the execution has completed in sync mode
     */
    class Sync implements ExecutionResult {

        private final Object result;
        private final Throwable throwable;

        public Sync(Object result, Throwable throwable) {
            this.result = result;
            this.throwable = throwable;
        }

        /**
         * set the callback of the execution. the implementing method must ensure that whenever this set callback is called
         * that callback must be called, even if the execution has already completed.
         *
         * @param callback execution callback
         */
        @Override
        public void setCallback(ExecutionCallback callback) {
            callback.onCompleted(result, throwable);
        }
    }

    /**
     * execution has succeeded
     * @param result result of the execution
     * @return an execution result used to retrieve the actual result
     */
    static ExecutionResult success(Object result) {
        return complete(result, null);
    }

    /**
     * the execution has failed
     * @param throwable exception thrown while executing
     * @return an execution result used to retrieve the actual result
     */
    static ExecutionResult fail(Throwable throwable) {
        if (throwable == null) {
            throw new NullPointerException("throwable cannot be null");
        }
        return complete(null, throwable);
    }

    /**
     * the execution has completed (succeeded or failed)
     * @param result the result of the execution (if succeeded)
     * @param throwable the exception thrown while executing (if failed)
     * @return an execution result used to retrieve the actual result
     */
    static ExecutionResult complete(Object result, Throwable throwable) {
        return new Sync(result, throwable);
    }

    /**
     * create an execution result from a completable future
     * @param future the future be be converted
     * @return the created execution result
     */
    static ExecutionResult from(@Nullable CompletableFuture<?> future) {
        if (future == null) {
            return null;
        }

        Async async = new Async();
        future.handle((r, e) -> {
            async.complete(r, e);
            return null;
        });

        return async;
    }
}
