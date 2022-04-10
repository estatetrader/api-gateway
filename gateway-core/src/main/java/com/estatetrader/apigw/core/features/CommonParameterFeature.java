package com.estatetrader.apigw.core.features;

import com.estatetrader.annotation.DefineCommonParameter;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.phases.executing.access.CallStarted;
import com.estatetrader.apigw.core.phases.parsing.CommonInfoParser;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.IllegalApiDefinitionException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public interface CommonParameterFeature {
    @Extension
    class CommonInfoParserImpl implements CommonInfoParser {
        /**
         * 解析基本信息
         *
         * @param schema 用于接收解析结果
         */
        @Override
        public void parse(ApiSchema schema) {
            Field[] fields = CommonParameter.class.getDeclaredFields();
            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
                    continue;
                }

                DefineCommonParameter a = field.getAnnotation(DefineCommonParameter.class);
                if (a == null) {
                    continue;
                }

                if (field.getType() != String.class) {
                    throw new IllegalApiDefinitionException("the type of field " + field +
                        " for an @DefineCommonParameter must be of type String");
                }

                String name;
                try {
                    name = (String) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }

                if (name == null || name.isEmpty() || !name.startsWith("_")) {
                    throw new IllegalApiDefinitionException("invalid common parameter name: " + name);
                }

                if (schema.commonParameterInfoMap.containsKey(name)) {
                    throw new IllegalApiDefinitionException("duplicate common parameter " + name + " found");
                }

                CommonParameterInfo info = new CommonParameterInfo(name, a.desc(), a.fromClient(), a.injectable());
                schema.commonParameterInfoMap.put(name, info);
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
            CommonParameterInfo cp = context.apiSchema.commonParameterInfoMap.get(info.name);
            if (cp == null || !cp.isInjectable() || !cp.isFromClient()) {
                return next.go();
            }

            return context.request.getParameter(cp.getName());
        }
    }
}
