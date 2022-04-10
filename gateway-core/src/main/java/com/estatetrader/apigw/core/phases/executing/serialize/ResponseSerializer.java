package com.estatetrader.apigw.core.phases.executing.serialize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.IOUtils;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.alibaba.fastjson.JSON.DEFAULT_GENERATE_FEATURE;

/**
 * API返回值和请求返回值的序列化器
 */
public interface ResponseSerializer {
    /**
     * 将对象序列化为JSON并写入输出流中
     * @param stream 接收序列化结果的流
     * @param object 要进行序列化的对象
     * @param methodCall 当前进行序列化的API
     * @param context 当前请求上下文
     * @throws IOException 写入stream时可能遇到的异常
     */
    void toJson(OutputStream stream, Object object, ApiMethodCall methodCall, ApiContext context) throws IOException;

    /**
     * 将对象序列化为JSON字符串
     * @param object 要进行序列化的对象
     * @param methodCall 当前进行序列化的API
     * @param context 当前请求上下文
     * @return 返回序列化后的JSON字符串
     */
    String toJsonString(Object object, ApiMethodCall methodCall, ApiContext context);

    @Extension
    class DefaultResponseSerializer implements ResponseSerializer {

        private final Extensions<SerializingConfigurer> serializingConfigurers;

        public DefaultResponseSerializer(Extensions<SerializingConfigurer> serializingConfigurers) {
            this.serializingConfigurers = serializingConfigurers;
        }

        /**
         * 将对象序列化为JSON并写入输出流中
         *
         * @param stream  接收序列化结果的流
         * @param object  要进行序列化的对象
         * @param methodCall 当前进行序列化的API
         * @param context 当前请求上下文
         * @throws IOException 写入stream时可能遇到的异常
         */
        @Override
        public void toJson(OutputStream stream, Object object, ApiMethodCall methodCall, ApiContext context) throws IOException {
            FilterFeatures ff = getFilterFeatures(object, methodCall, context);
            JSON.writeJSONString(
                stream,
                IOUtils.UTF8,
                object,
                SerializeConfig.globalInstance,
                ff.filters,
                null,
                DEFAULT_GENERATE_FEATURE,
                ff.features
            );
        }

        /**
         * 将对象序列化为JSON字符串
         *
         * @param object  要进行序列化的对象
         * @param methodCall 当前进行序列化的API
         * @param context 当前请求上下文
         * @return 返回序列化后的JSON字符串
         */
        @Override
        public String toJsonString(Object object, ApiMethodCall methodCall, ApiContext context) {
            FilterFeatures ff = getFilterFeatures(object, methodCall, context);
            return JSON.toJSONString(
                object,
                SerializeConfig.globalInstance,
                ff.filters,
                null,
                DEFAULT_GENERATE_FEATURE,
                ff.features
            );
        }

        private FilterFeatures getFilterFeatures(Object object, ApiMethodCall methodCall, ApiContext context) {
            List<SerializeFilter> filters = new ArrayList<>(serializingConfigurers.size());
            List<SerializerFeature> features = new ArrayList<>(serializingConfigurers.size());

            for (SerializingConfigurer configurer : serializingConfigurers) {
                filters.addAll(configurer.filters(object, methodCall, context));
                features.addAll(configurer.features(methodCall, context));
            }

            return new FilterFeatures(filters, features);
        }

        private static class FilterFeatures {
            final SerializeFilter[] filters;
            final SerializerFeature[] features;

            public FilterFeatures(List<SerializeFilter> filters, List<SerializerFeature> features) {
                this.filters = filters.toArray(new SerializeFilter[0]);
                this.features = features.toArray(new SerializerFeature[0]);
            }
        }
    }
}
