package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.annotation.InjectRequestHeader;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 请求头注入相关的功能
 */
public interface RequestHeaderInjectFeature {

    @Extension
    class ParameterInfoParserImpl implements ParsingClass.ParameterInfoParser {
        @Override
        public boolean parse(Class<?> clazz,
                             Method method,
                             ApiMethodInfo apiInfo,
                             Parameter parameter,
                             ApiParameterInfo pInfo) {
            InjectRequestHeader a = parameter.getAnnotation(InjectRequestHeader.class);
            if (a == null) {
                return false;
            }
            if (a.value().isEmpty()) {
                throw new IllegalApiDefinitionException("the specified header name must not be empty");
            }
            pInfo.serverOnly = true;
            pInfo.name = CommonParameter.requestHeaderInjectPrefix + a.value();
            pInfo.injectArg = a.value();
            pInfo.isRequired = a.required();
            pInfo.isAutowired = true;

            return true;
        }
    }

    @Extension
    class AutowiredParameterValueProviderImpl implements CallStarted.AutowiredParameterValueProvider {
        /**
         * 注入可注入的参数
         * <p>
         * 根据call和info判断你是否能够为当前参数提供注入，并将最终注入结果作为返回值返回出去
         * 如果你不能为当前参数提供注入，则调用next.go()将机会留给其他注入器
         *
         * @param call    当前待注入的API call
         * @param info    当前待注入的参数信息
         * @param context 请求上下文
         * @param next    如果你需要其他注入器提供注入，则调用next.go()
         * @return 参数最终要注入的值
         * @throws GatewayException 抛出错误码
         */
        @Override
        public String autowire(ApiMethodCall call,
                               ApiParameterInfo info,
                               ApiContext context,
                               Next<String, GatewayException> next) throws GatewayException {
            if (info.name != null && info.name.startsWith(CommonParameter.requestHeaderInjectPrefix)) {
                return context.request.getHeader(info.injectArg);
            } else {
                return next.go();
            }
        }
    }
}
