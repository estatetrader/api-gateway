package com.estatetrader.apigw.core.features;

import com.alibaba.fastjson.JSON;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.annotation.SubsystemParamsAutowired;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 子系统参数相关功能，此子系统参数主要由客户端维护
 */
public interface SubsystemParametersFeature {

    @Extension
    class ParameterInfoParserImpl implements ParsingClass.ParameterInfoParser {
        @Override
        public boolean parse(Class<?> clazz,
                             Method method,
                             ApiMethodInfo apiInfo,
                             Parameter parameter,
                             ApiParameterInfo pInfo) {
            SubsystemParamsAutowired a = parameter.getAnnotation(SubsystemParamsAutowired.class);
            if (a == null) {
                return false;
            }

            pInfo.serverOnly = true;
            pInfo.name = CommonParameter.subsystemParams;
            pInfo.isRequired = false;
            pInfo.isAutowired = true;

            if (!Map.class.isAssignableFrom(parameter.getType())) {
                throw new IllegalApiDefinitionException("parameter " + pInfo.name + " must be of Map type " +
                    "for subsystem common params");
            }

            return true;
        }
    }

    @Extension
    class ParameterParserImpl implements RequestStarted.ParameterParser {

        private static final Logger logger = LoggerFactory.getLogger(SubsystemParametersFeature.class);

        @Override
        public void parse(ApiContext context) {
            String scp = context.request.getParameter(CommonParameter.subsystemParams);
            if (scp == null || scp.isEmpty()) {
                context.subsystemParams = null;
            } else {
                try {
                    Object obj = JSON.parse(scp);
                    if (obj instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) obj;
                        context.subsystemParams = new HashMap<>(map.size());
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            String key = String.valueOf(entry.getKey());
                            String value = String.valueOf(entry.getValue());
                            context.subsystemParams.put(key, value);
                        }
                    } else {
                        logger.warn("subsystem common parameters {} is not of map type", scp);
                        context.subsystemParams = Collections.emptyMap();
                    }
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("failed to parse subsystem common parameters from " + scp, e);
                    }
                    context.subsystemParams = Collections.emptyMap();
                }
            }
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
            if (CommonParameter.subsystemParams.equals(info.name) && context.subsystemParams != null) {
                return JSON.toJSONString(context.subsystemParams);
            }

            return next.go();
        }
    }
}
