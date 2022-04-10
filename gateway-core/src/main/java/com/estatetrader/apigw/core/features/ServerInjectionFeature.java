package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.JSON;
import com.estatetrader.annotation.*;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.ConstField;
import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.util.Lambda;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;

/**
 * 服务端注入，用于API之间依赖
 */
public interface ServerInjectionFeature {

    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {

        @Override
        public void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo apiInfo, ServiceInstance serviceInstance) {
            @SuppressWarnings("deprecation")
            ParamsExport pe = method.getAnnotation(ParamsExport.class);
            if (pe != null && pe.value().length > 0) {
                apiInfo.exportParams = new HashMap<>(pe.value().length);
                //noinspection deprecation
                for (ParamExport n : pe.value()) {
                    apiInfo.exportParams.put(n.name(), n.dataType());
                }
            }

            @SuppressWarnings("deprecation")
            ParamExport pei = method.getAnnotation(ParamExport.class);
            if (pei != null) {
                if (apiInfo.exportParams == null) {
                    apiInfo.exportParams = new HashMap<>();
                    apiInfo.exportParams.put(pei.name(), pei.dataType());
                }
            }

            ExportParams eps = method.getAnnotation(ExportParams.class);
            if (eps != null && eps.value().length > 0) {
                apiInfo.exportParams = new HashMap<>(eps.value().length);
                for (ExportParam n : eps.value()) {
                    apiInfo.exportParams.put(n.name(), n.dataType());
                }
            }
            ExportParam ep = method.getAnnotation(ExportParam.class);
            if (ep != null) {
                if (apiInfo.exportParams == null) {
                    apiInfo.exportParams = new HashMap<>();
                    apiInfo.exportParams.put(ep.name(), ep.dataType());
                }
            }
        }
    }

    @Extension
    class ParseParameterHandlerImpl implements ParsingClass.ParseParameterHandler {

        @Override
        public void parseParameter(Class<?> clazz, Method method, ApiMethodInfo apiInfo, Parameter parameter, ApiParameterInfo pInfo) {
            Annotation[] annotations = parameter.getAnnotations();

            ImportParam importParam = (ImportParam) Lambda.find(annotations, a -> a.annotationType() == ImportParam.class);
            if (importParam != null) {
                pInfo.injectable = Lambda.newInstance(importParam.serviceInject());
                pInfo.injectable.setName(importParam.value());
            }

            @SuppressWarnings("deprecation")
            InjectParam injectParam = (InjectParam) Lambda.find(annotations, a -> a.annotationType() == InjectParam.class);
            if (injectParam != null) {
                pInfo.injectable = Lambda.newInstance(injectParam.serviceInject());
                pInfo.injectable.setName(injectParam.value());
            }

            InjectID injectId = (InjectID) Lambda.find(annotations, a -> a.annotationType() == InjectID.class);
            if (injectId != null) {
                pInfo.injectable = Lambda.newInstance(injectId.serviceInject());
                pInfo.injectable.setName(injectId.value());
            }

            @SuppressWarnings("deprecation")
            InjectIDList injectIdList = (InjectIDList) Lambda.find(annotations, a -> a.annotationType() == InjectIDList.class);
            if (injectIdList != null) {
                pInfo.injectable = Lambda.newInstance(injectIdList.serviceInject());
                pInfo.injectable.setName(injectIdList.value());
            }

            InjectIDs injectIds = (InjectIDs) Lambda.find(annotations, a -> a.annotationType() == InjectIDs.class);
            if (injectIds != null) {
                pInfo.injectable = Lambda.newInstance(injectIds.serviceInject());
                pInfo.injectable.setName(injectIds.value());
            }

            ApiParameter apiParameter = (ApiParameter) Lambda.find(annotations, a -> a.annotationType() == ApiParameter.class);
            if (apiParameter != null &&
                    apiParameter.serviceInject() != ServiceInjectable.class) {

                pInfo.injectable = Lambda.newInstance(apiParameter.serviceInject());
                pInfo.isRequired = false;
            }

            ApiAutowired apiAutowired = (ApiAutowired) Lambda.find(annotations, a -> a.annotationType() == ApiAutowired.class);
            if (apiAutowired != null &&
                    CommonParameter.serviceInjection.equals(apiAutowired.value()) &&
                    apiAutowired.serviceInject() != ServiceInjectable.class) {

                pInfo.injectable = Lambda.newInstance(apiAutowired.serviceInject());
                pInfo.isRequired = false;
            }
        }
    }

    @Extension
    class ParameterValueProviderImpl implements CallStarted.ParameterValueProvider {
        @Override
        public String provide(ApiMethodCall call, ApiParameterInfo info, String value, ApiContext context) {
            if (info.injectable == null || call.prev == null) {
                return value;
            }

            String key = info.injectable.getName();
            ServiceInjectable.InjectionData injectionData = Lambda.newInstance(info.injectable.getDataType());
            // 合并该调用所有依赖项中的 key 键对应的值
            for (ApiMethodCall dependency : call.prev) {
                if (dependency.exportParams != null && dependency.exportParams.containsKey(key)) {
                    String notification = dependency.exportParams.get(key);
                    ServiceInjectable.InjectionData toMerge;
                    try {
                        toMerge = JSON.parseObject(notification, info.injectable.getDataType());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("service injection failed. notification 解析失败: " + notification
                            + " from method:" + dependency.method.methodName, e);
                    }
                    injectionData.batchMerge(toMerge);
                }
            }

            Object data = injectionData.getValue();
            if (data == null) {
                return value;
            }
            if (data instanceof String) {
                return (String) data;
            }
            return JSON.toJSONString(data);
        }
    }

    @Extension
    class NotificationProcessorImpl implements CallResultReceived.NotificationProcessor {
        /**
         * 处理来自后端服务器返回的旁路信息
         * 如果你对某个信息不感兴趣，请调用next.go()让其他处理器处理
         *
         * @param name    旁路信息的名称
         * @param value   信息的内容
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param next    调用next.go()让其他旁路信息处理器处理
         * @throws IOException 抛出异常
         */
        @Override
        public void process(String name,
                            String value,
                            ApiContext context,
                            ApiMethodCall call,
                            Next.NoResult<IOException> next) throws IOException {

            if (name.startsWith(ConstField.SERVICE_PARAM_EXPORT_PREFIX)) {
                if (call.exportParams == null) {
                    call.exportParams = new HashMap<>();
                }
                call.exportParams.put(name.substring(ConstField.SERVICE_PARAM_EXPORT_PREFIX.length()), value);
                return;
            }

            next.go();
        }
    }
}
