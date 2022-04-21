package com.estatetrader.apigw.core.phases.executing.serialize;

import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * hook to configure the process of serializing to client
 */
public interface SerializingConfigurer {
    /**
     * serialize filters should be used when serializing api result to client
     *
     * @param object the object to be serialized
     * @param methodCall the api to serialize
     * @param context context of this very request
     * @return serialize filters (FastJson)
     */
    default List<SerializeFilter> filters(Object object, ApiMethodCall methodCall, ApiContext context) {
        return Collections.emptyList();
    }

    /**
     * serialize features should be used when serializing api result to client
     *
     * @param methodCall the api to serialize
     * @param context context of this very request
     * @return serialize features (FastJson)
     */
    default List<SerializerFeature> features(ApiMethodCall methodCall, ApiContext context) {
        return Collections.emptyList();
    }

    @Extension
    class DefaultSerializingConfigurer implements SerializingConfigurer {

        private final List<SerializerFeature> FEATURES = Arrays.asList(
            SerializerFeature.DisableCircularReferenceDetect,//disable循环引用
            SerializerFeature.WriteMapNullValue,//null属性，序列化为null,do by guankaiqiang,android sdk中 JSON.optString()将null convert成了"null",故关闭该特性
            SerializerFeature.NotWriteRootClassName, //与pocrd保持一致
            //            SerializerFeature.WriteEnumUsingToString, //与pocrd保持一致
//            SerializerFeature.WriteNullNumberAsZero,//与pocrd保持一致
//            SerializerFeature.WriteNullBooleanAsFalse,//与pocrd保持一致
//            SerializerFeature.WriteNullStringAsEmpty, //配置SerializerFeature.WriteMapNullValue 后需要同时配置， 使null变成空字符串
//            SerializerFeature.WriteNullListAsEmpty,
            SerializerFeature.WriteDateUseDateFormat
        );

        /**
         * serialize features should be used when serializing api result to client
         *
         *
         * @param methodCall the api to serialize
         * @param context context of this very request
         * @return serialize features (FastJson)
         */
        @Override
        public List<SerializerFeature> features(ApiMethodCall methodCall, ApiContext context) {
            return FEATURES;
        }
    }
}
