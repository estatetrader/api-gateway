package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.ApiParameter;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.annotation.HttpApi;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public interface ExampleValueFeature {
    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {
        @Override
        public void parseMethodBrief(Class<?> clazz,
                                     Method method,
                                     ApiMethodInfo apiInfo,
                                     ServiceInstance serviceInstance) {
            HttpApi api = method.getAnnotation(HttpApi.class);
            if (api != null && !api.exampleValue().isEmpty()) {
                apiInfo.exampleValue = api.exampleValue();
            }
        }
    }

    @Extension
    class ParseParameterHandlerImpl implements ParsingClass.ParseParameterHandler {
        @Override
        public void parseParameter(Class<?> clazz,
                                   Method method,
                                   ApiMethodInfo apiInfo,
                                   Parameter parameter,
                                   ApiParameterInfo pInfo) {
            ApiParameter p = parameter.getAnnotation(ApiParameter.class);
            if (p != null && !p.exampleValue().isEmpty()) {
                pInfo.exampleValue = p.exampleValue();
            }
        }
    }
}