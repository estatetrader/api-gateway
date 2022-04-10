package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.estatetrader.annotation.*;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.models.inject.DatumConsumerSpec;
import com.estatetrader.apigw.core.models.inject.DatumProvidedValue;
import com.estatetrader.apigw.core.models.inject.DatumProviderSpec;
import com.estatetrader.apigw.core.models.inject.DatumWrapper;
import com.estatetrader.apigw.core.phases.executing.access.CallResultReceived;
import com.estatetrader.apigw.core.phases.executing.serialize.SerializingConfigurer;
import com.estatetrader.apigw.core.phases.parsing.ApiRegister;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.define.ResponseFilter;
import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.gateway.StructTypeResolver;
import com.estatetrader.util.Lambda;
import com.estatetrader.algorithm.workflow.WorkflowPipeline;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 返回值拦截器，主要用于API注入
 */
public interface ResponseFilterFeature {

    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {

        private final StructTypeResolver typeResolver = new StructTypeResolver();

        @Override
        public void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo info, ServiceInstance serviceInstance) {
            List<ResponseFilter> responseFilters = new ArrayList<>();
            FilterResponse filterResponse = method.getAnnotation(FilterResponse.class);
            if (filterResponse != null) {
                responseFilters.add(Lambda.newInstance(filterResponse.type()));
            }
            FilterResponseRepeated filterResponseRepeated = method.getAnnotation(FilterResponseRepeated.class);
            if (filterResponseRepeated != null) {
                for (FilterResponse fr : filterResponseRepeated.value()) {
                    responseFilters.add(Lambda.newInstance(fr.type()));
                }
            }

            ResponseInjectFromApi responseInjectFromApi = method.getAnnotation(ResponseInjectFromApi.class);
            if (responseInjectFromApi != null) {
                InjectFromApi inject = new GenericInjector(responseInjectFromApi.value());
                responseFilters.add(inject);
            }
            ResponseInjectFromApiRepeated responseInjectFromApiRepeated = method.getAnnotation(ResponseInjectFromApiRepeated.class);
            if (responseInjectFromApiRepeated != null) {
                for (ResponseInjectFromApi fr : responseInjectFromApiRepeated.value()) {
                    InjectFromApi inject = new GenericInjector(fr.value());
                    responseFilters.add(inject);
                }
            }

            if (!responseFilters.isEmpty()) {
                info.responseFilters = responseFilters;
            }

            ResponseInjectProvider responseInjectProvider = method.getAnnotation(ResponseInjectProvider.class);
            if (responseInjectProvider != null) {
                info.responseInjectProviderName = responseInjectProvider.value();
                info.datumProviderSpec = DatumProviderSpec.parse(method, info.responseInjectProviderName, typeResolver);
                if (info.datumProviderSpec == null) {
                    throw new IllegalApiDefinitionException("在指定@ResponseInjectProvider(" +
                        info.responseInjectProviderName + ")时须同时使用@DefineDatum将其返回值声明为Datum定义");
                }
            }
        }
    }

    @Extension
    class ApiInfoRegisterImplForProvider implements ApiRegister {
        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            String providerName = info.responseInjectProviderName;
            if (providerName != null) {
                if (schema.responseInjectProviderMap.containsKey(providerName)) {
                    throw new IllegalApiDefinitionException("Duplicate definition for inject-provider " + providerName +
                            ". It is already defined in method " + schema.responseInjectProviderMap.get(providerName).methodName);
                }
                schema.responseInjectProviderMap.put(providerName, info);
            }
        }
    }

    @Extension(after = ApiInfoRegisterImplForProvider.class)
    class ApiInfoRegisterImplForConsumer implements ApiRegister {
        private final StructTypeResolver typeResolver = new StructTypeResolver();

        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            Map<String, DatumProviderSpec> providerSpecMap = new LinkedHashMap<>();
            for (InjectFromApi inject : InjectFromApi.getInjectFromApiFilters(info)) {
                String providerName = inject.getFromApiName();
                ApiMethodInfo provider = schema.responseInjectProviderMap.get(providerName);
                if (provider == null) {
                    throw new IllegalApiDefinitionException("could not find response inject provider " + providerName);
                }
                DatumProviderSpec providerSpec = provider.datumProviderSpec;
                providerSpecMap.put(providerSpec.getDatumType(), providerSpec);
            }
            DatumConsumerSpec consumerSpec = DatumConsumerSpec.parseConsumerSpec(info.proxyMethodInfo, providerSpecMap, typeResolver);
            // 另外一种场景（声明了注入规则却没有声明@ResponseInjectFromApi）会在上述的parseConsumerSpec中涵盖到
            // 这里仅需检查使用了@ResponseInjectFromApi却没有声明注入规则的情况
            if (consumerSpec == null && !providerSpecMap.isEmpty()) {
                // definitionMap为空意味着本API没有定义任何@ResponseInjectFromApi
                throw new IllegalApiDefinitionException("在使用@ResponseInjectFromApi时应同时" +
                    "在返回值中使用@ImportDatum和@ExposeDatumKey定义具体的注入规则");
            }
            info.datumConsumerSpec = consumerSpec;
        }
    }

    @Extension(after = ServerInjectionFeature.class)
    class AfterApiCallResultReceivedImpl implements CallResultReceived.AfterApiCallResultReceived {
        @Override
        public void receive(ApiMethodCall call, ApiContext context, WorkflowPipeline pipeline) throws Exception {
            Object result = call.result;

            if (result == null || call.method.responseFilters == null || call.disableResponseFilters) {
                return;
            }

            for (ResponseFilter filter : call.method.responseFilters) {
                if (filter instanceof InjectFromApi) {
                    InjectFromApi inject = (InjectFromApi) filter;
                    ApiMethodInfo method = context.apiSchema.responseInjectProviderMap.get(inject.getFromApiName());
                    if (method == null) {
                        throw new GatewayException(ApiReturnCode.UNKNOWN_DEPENDENT_METHOD);
                    }

                    List<String> requiredParams = new ArrayList<>();
                    Object[] args = new Object[method.parameterInfos.length];
                    for (int i = 0; i < method.parameterInfos.length; i++) {
                        ApiParameterInfo p = method.parameterInfos[i];
                        if (p.injectable != null && call.exportParams != null) {
                            args[i] = getExportedParameter(call, p.injectable);
                        }
                        if (!p.isAutowired && args[i] == null) {
                            // 兼容老的注入逻辑，仅求解尚未赋值的参数
                            requiredParams.add(p.nativeName);
                        }
                    }
                    Map<String, Object> exposedValues = inject.expose(call.method, result, requiredParams, method);
                    for (int i = 0; i < method.parameterInfos.length; i++) {
                        ApiParameterInfo p = method.parameterInfos[i];
                        Object exposedValue = exposedValues.get(p.nativeName);
                        if (exposedValue != null) {
                            args[i] = exposedValue;
                        }
                    }

                    new ApiCallExecutorImpl(pipeline, context.executeApiCall).start(method, args, dependentResult -> {
                        synchronized (result) {
                            inject.inject(call.method, result, method, dependentResult);
                        }
                    });
                } else {
                    synchronized (result) {
                        filter.filter(result);
                    }
                }
            }
        }

        private Object getExportedParameter(ApiMethodCall call, ServiceInjectable injectable) {
            if (call.exportParams == null) {
                return null;
            }
            String text = call.exportParams.get(injectable.getName());
            if (text == null) {
                return null;
            }
            return JSON.parseObject(text, injectable.getDataType()).getValue();
        }
    }

    @Extension
    class ApiResultSerializingConfigurer implements SerializingConfigurer {

        private final List<SerializeFilter> datumWrapperFilters = Collections.singletonList(new DatumWrapperFilter());

        /**
         * serialize filters should be used when serializing api result to client
         *
         * @param object the object to be serialized
         * @param methodCall the api to serialize
         * @param context context of this very request
         * @return serialize filters (FastJson)
         */
        @Override
        public List<SerializeFilter> filters(Object object, ApiMethodCall methodCall, ApiContext context) {
            if (methodCall == null || methodCall.method.datumConsumerSpec == null) {
                return Collections.emptyList();
            } else {
                // 仅在有API返回值注入时，才需执行datum-wrapper的filter，已提高性能
                return datumWrapperFilters;
            }
        }

        /**
         * 去除DatumWrapper，使其内部包装的实际值直接暴露给客户端
         */
        private static class DatumWrapperFilter implements ValueFilter {
            @Override
            public Object process(Object object, String name, Object value) {
                if (value instanceof DatumWrapper) {
                    return ((DatumWrapper) value).getValue();
                } else if (value instanceof Collection) {
                    return convertCollection((Collection<?>) value);
                } else {
                    return value;
                }
            }

            private Collection<?> convertCollection(Collection<?> collection) {
                List<Object> result = null;
                for (Object item : collection) {
                    if (item instanceof DatumWrapper) {
                        result = new ArrayList<>(collection);
                        break;
                    }
                }
                if (result == null) {
                    return collection;
                }
                for (int i = 0; i < result.size(); i++) {
                    Object item = result.get(i);
                    if (item instanceof DatumWrapper) {
                        result.set(i, ((DatumWrapper) item).getValue());
                    }
                }
                return result;
            }
        }
    }

    interface InjectFromApi extends ResponseFilter {
        static List<InjectFromApi> getInjectFromApiFilters(ApiMethodInfo methodInfo) {
            if (methodInfo.responseFilters == null) {
                return Collections.emptyList();
            }
            return methodInfo.responseFilters.stream()
                .filter(x -> x instanceof InjectFromApi)
                .map(x -> (InjectFromApi) x)
                .collect(Collectors.toList());
        }

        String getFromApiName();

        /**
         * 被依赖的接口在执行时需要的非autowired参数由本函数提供
         * 被依赖的接口会在被拦截的接口执行成功后执行
         * @param consumer 返回值注入的消费方
         * @param provider  返回值注入的提供方
         * @param requiredParams 需要求解的参数列表
         * @param response      被拦截的接口的返回值
         * @return 返回参数的值
         */
        Map<String, Object> expose(ApiMethodInfo consumer, Object response, List<String> requiredParams, ApiMethodInfo provider);

        /**
         * 在当前接口和被依赖的接口都执行完成后执行该函数
         * @param consumer 返回值注入的消费方
         * @param response 当前要拦截的接口的返回值
         * @param provider  返回值注入的提供方
         * @param dependentResponse 被依赖的接口的返回值
         */
        void inject(ApiMethodInfo consumer, Object response, ApiMethodInfo provider, Object dependentResponse) throws GatewayException;

        /**
         * 无需实现该函数，网关会调用 void filter(Map<String, Object> options, Object response, Object dependentResponse)
         * @param response      被拦截的接口的返回值
         */
        default void filter(Object response) {
            throw new UnsupportedOperationException();
        }
    }

    class GenericInjector implements InjectFromApi {

        private final String fromApiName;

        public GenericInjector(String fromApiName) {
            this.fromApiName = fromApiName;
        }

        @Override
        public String getFromApiName() {
            return fromApiName;
        }

        /**
         * 被依赖的接口在执行时需要的非autowired参数由本函数提供
         * 被依赖的接口会在被拦截的接口执行成功后执行
         *
         * @param consumer       返回值注入的消费方
         * @param response       被拦截的接口的返回值
         * @param requiredParams 需要求解的参数列表
         * @param provider       返回值注入的提供方
         * @return 返回参数的值
         */
        @Override
        public Map<String, Object> expose(ApiMethodInfo consumer, Object response, List<String> requiredParams, ApiMethodInfo provider) {
            if (response == null) {
                return Collections.emptyMap();
            }
            String datumType = provider.datumProviderSpec.getDatumType();
            return consumer.datumConsumerSpec.export(datumType, response);
        }

        /**
         * 在当前接口和被依赖的接口都执行完成后执行该函数
         *
         * @param consumer          返回值注入的消费方
         * @param response          当前要拦截的接口的返回值
         * @param provider          返回值注入的提供方
         * @param dependentResponse 被依赖的接口的返回值
         */
        @Override
        public void inject(ApiMethodInfo consumer, Object response, ApiMethodInfo provider, Object dependentResponse) {
            if (response == null || dependentResponse == null) {
                return;
            }
            String datumType = provider.datumProviderSpec.getDatumType();
            DatumProvidedValue providedValue = provider.datumProviderSpec.provideValue(dependentResponse);
            consumer.datumConsumerSpec.inject(datumType, response, providedValue);
        }
    }
}
