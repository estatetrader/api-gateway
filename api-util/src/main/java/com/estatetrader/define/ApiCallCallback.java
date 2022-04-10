package com.estatetrader.define;

import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.core.GatewayException;

/**
 * callback which will be called when an api call has completed
 */
@FunctionalInterface
public interface ApiCallCallback<T> {
    /**
     * called when the api call completed
     * @param result the result of the api call
     * @param code the return code of the api call
     * @throws Throwable
     */
    void onCompleted(T result, int code) throws Throwable;

    @FunctionalInterface
    interface Complex<T> {
        /**
         * called when the api call completed
         * @param call the result call
         * @throws Throwable
         */
        void onCompleted(T result, int code, ApiCallInfo call) throws Throwable;
    }

    @FunctionalInterface
    interface Simple<T> extends ApiCallCallback<T> {
        /**
         * called when the api call completed
         * @param result the result of the api call
         * @param code the return code of the api call
         * @throws Throwable
         */
        default void onCompleted(T result, int code) throws Throwable {
            if (code != 0) {
                throw new GatewayException(ApiReturnCode.DEPENDENT_API_FAILURE);
            }
            onCompleted(result);
        }

        /**
         * called when the api call completed
         * @param result the result of the api call
         * @throws Throwable
         */
        void onCompleted(T result) throws Throwable;

        @FunctionalInterface
        interface Two<T1, T2> {
            /**
             * called when the api call completed
             * @param r1 the result of the first api
             * @param r2 the result of the second api
             * @throws Throwable
             */
            void onCompleted(T1 r1, T2 r2) throws Throwable;
        }
    }
}
