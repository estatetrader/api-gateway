package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.ApiAutowired;
import com.estatetrader.annotation.ApiParameter;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.AuthenticationService;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.EnumNull;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.util.Lambda;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiParameterInfo;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.responseEntity.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 服务端授权相关功能
 */
public interface AuthenticationServiceFeature {

    @Extension(after = SecurityFeature.class)
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {

        @Override
        public void parseMethodBrief(Class<?> clazz, Method method,
                                     ApiMethodInfo apiInfo,
                                     ServiceInstance serviceInstance) {
            if (AuthenticationService.class.isAssignableFrom(clazz)) {
                for (Method m : AuthenticationService.class.getDeclaredMethods()) {
                    if (Lambda.equals(m, method)) {
                        apiInfo.authenticationMethod = true;
                    }
                }
            }
        }

        @Override
        public void parseMethodParameters(Class<?> clazz, Method method, ApiMethodInfo apiInfo) {
            // 校验授权接口注释是否符合规范

            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            if (apiInfo.authenticationMethod) {
                if (parameterAnnotations[0].length == 0
                        || parameterAnnotations[0][0].annotationType() != ApiAutowired.class
                        || !CommonParameter.userId.equals(((ApiAutowired)parameterAnnotations[0][0]).value())) {
                    throw new IllegalApiDefinitionException("authentication method must tag the first parameter as Autowired(userid).");
                }
                if (parameterAnnotations[1].length == 0
                        || parameterAnnotations[1][0].annotationType() != ApiParameter.class
                        || ((ApiParameter)parameterAnnotations[1][0]).enumDef() == EnumNull.class) {
                    throw new IllegalApiDefinitionException("authentication method must tag the second parameter as ApiParameter with a enum define.");
                }
                if (parameterAnnotations[2].length == 0
                        || parameterAnnotations[2][0].annotationType() != ApiParameter.class) {
                    throw new IllegalApiDefinitionException("authentication method must tag the third parameter as ApiParameter");
                }
            }
        }
    }

    @Extension(before = SecurityFeature.class) // userId will be set in SecurityFeature, so we need process before it
    class AutowiredParameterValueProviderImpl implements CallStarted.AutowiredParameterValueProvider {

        Logger logger = LoggerFactory.getLogger(AuthenticationServiceFeature.class);

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

            if (!CommonParameter.userId.equals(info.name) || call.prev == null) {
                return next.go();
            }

            for (ApiMethodCall dependency : call.prev) {
                if (!dependency.method.authenticationMethod) {
                    continue;
                }

                AuthenticationResult authenticationResult = (AuthenticationResult) dependency.result;

                if (authenticationResult == null) {
                    throw new GatewayException(ApiReturnCode.DEPENDENT_API_FAILURE);
                }

                if (authenticationResult.apis.contains(call.method.methodName)) {
                    return String.valueOf(authenticationResult.authorizedUserId);
                } else {
                    String text = String.join(", ", authenticationResult.apis);
                    logger.error("authentication service {} only allows these APIs: {}, so visiting to {} is denied",
                        dependency.method.methodName, text, call.method.methodName);

                    throw new GatewayException(ApiReturnCode.ACCESS_DENIED);
                }
            }

            // let others have a try
            return next.go();
        }
    }
}
