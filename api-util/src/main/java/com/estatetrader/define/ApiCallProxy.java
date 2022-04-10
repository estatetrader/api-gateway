package com.estatetrader.define;

import com.estatetrader.annotation.DocumentSkip;

import java.util.Set;
import java.util.function.Function;

/**
 * add a parameter of this type to your parameter list in the mix function, if you want to execute some APIs on the fly.
 */
@DocumentSkip
public interface ApiCallProxy {

    /**
     * optional list
     * @return optional list
     */
    Set<String> optional();

    /**
     * get an instance of a certain type
     * @param serviceType service type
     * @param <T> service type
     * @return instance of the service
     */
    <T> ApiCallService<T> service(Class<T> serviceType);

    /**
     * series of blocks which will be executed in order
     * @param blocks blocks to execute
     */
    void sequence(SequenceBlock... blocks);

    default <T1, R1> void api(Class<T1> serviceType1, Function<T1, R1> calling1, ApiCallCallback.Simple<R1> callback1) {
        service(serviceType1).api(calling1, callback1);
    }

    default <T1, R1, T2, R2> void api(Class<T1> serviceType1, Function<T1, R1> calling1, Class<T2> serviceType2, Function<T2, R2> calling2, ApiCallCallback.Simple.Two<R1, R2> callback1) {

    }

    /**
     * a service instance by which you can call api defined in it
     * @param <T> service type
     */
    interface ApiCallService<T> {
        /**
         * call an api
         * @param calling which api you want to call and its parameters to pass
         * @param callback what to do after we got the api result
         */
        <R> ApiCallService<T> api(Function<T, R> calling, ApiCallCallback<R> callback);

        /**
         * call an api
         * @param calling which api you want to call and its parameters to pass
         * @param callback what to do after we got the api result
         */
        default <R> ApiCallService<T> api(Function<T, R> calling, ApiCallCallback.Simple<R> callback) {
            return api(calling, (ApiCallCallback<R>) callback);
        }
    }

    /**
     * a block used to build a sequence
     */
    interface SequenceBlock {
        /**
         * the execution of the block
         * @param proxy a proxy used to build nested blocks
         * @throws Throwable throwable may be thrown
         */
        void run(ApiCallProxy proxy) throws Throwable;
    }
}
