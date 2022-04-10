package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.extensions.Next;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.phases.parsing.ApiRegister;
import com.estatetrader.apigw.core.phases.parsing.ApiVerifier;
import com.estatetrader.apigw.core.phases.parsing.ParsingClass;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.util.Lambda;
import com.estatetrader.annotation.ErrorCodeMapping;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodCall;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.apigw.core.models.ApiMethodInfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 错误码映射相关功能
 */
public interface ErrorCodeMappingFeature {

    @Extension
    class ParseMethodHandlerImpl implements ParsingClass.ParseMethodHandler {

        @Override
        public void parseMethodBrief(Class<?> clazz, Method method, ApiMethodInfo apiInfo, ServiceInstance serviceInstance) {
            ErrorCodeMapping a = method.getAnnotation(ErrorCodeMapping.class);
            if (a == null) return;

            // 获得一个从内部系统code指向当前系统code的映射表
            Map<Integer, Integer> map = new HashMap<>();
            String name = method.getName();
            parseMapping(map, a.mapping(), name);
            parseMapping(map, a.mapping1(), name);
            parseMapping(map, a.mapping2(), name);
            parseMapping(map, a.mapping3(), name);
            parseMapping(map, a.mapping4(), name);

            parseMapping(map, a.mapping5(), name);
            parseMapping(map, a.mapping6(), name);
            parseMapping(map, a.mapping7(), name);
            parseMapping(map, a.mapping8(), name);
            parseMapping(map, a.mapping9(), name);

            parseMapping(map, a.mapping10(), name);
            parseMapping(map, a.mapping11(), name);
            parseMapping(map, a.mapping12(), name);
            parseMapping(map, a.mapping13(), name);
            parseMapping(map, a.mapping14(), name);

            parseMapping(map, a.mapping15(), name);
            parseMapping(map, a.mapping16(), name);
            parseMapping(map, a.mapping17(), name);
            parseMapping(map, a.mapping18(), name);
            parseMapping(map, a.mapping19(), name);

            apiInfo.innerCodeMap = map;
        }

        private void parseMapping(Map<Integer, Integer> map, int[] mapping, String methodName) {
            if (mapping != null && mapping.length > 0) {
                if (mapping.length % 2 > 0) {
                    StringBuilder sb = new StringBuilder("invalid code mapping(1): ");
                    for (int m : mapping) {
                        sb.append(m).append(",");
                    }

                    throw new IllegalApiDefinitionException(sb.append(" in ").append(methodName).toString());
                }

                for (int i = 0; i < mapping.length; i += 2) {
                    if (mapping[i] <= 0 || mapping[i + 1] <= 0) {
                        throw new IllegalApiDefinitionException("invalid code mapping(2): " +
                            mapping[i] + "=>" + mapping[i + 1] + " in " + methodName);
                    }
                    if (map.containsKey(mapping[i + 1])) {
                        throw new IllegalApiDefinitionException("duplicate code mapping: " +
                            map.get(mapping[i + 1]));
                    }

                    map.put(mapping[i + 1], mapping[i]);
                }
            }
        }
    }

    @Extension(after = ErrorCodeFeature.class) // 在error code注册之后进行
    class ApiRegisterImpl implements ApiRegister {
        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            if (info.innerCodeMap == null) {
                return;
            }

            info.innerReturnCodeMap = new HashMap<>(info.innerCodeMap.size());
            for (Map.Entry<Integer, Integer> entry : info.innerCodeMap.entrySet()) {
                int mappedCode = entry.getValue();
                List<AbstractReturnCode> list = schema.returnCodes.get(mappedCode);
                if (list == null) {
                    throw new IllegalApiDefinitionException("undefined designed error code " +
                        mappedCode + " in code mapping");
                }

                AbstractReturnCode rc = Lambda.find(list, x -> Objects.equals(x.getService(), info.groupName));
                if (rc == null) {
                    rc = list.get(0);
                }

                info.innerReturnCodeMap.put(entry.getKey(), rc);
            }
        }
    }

    @Extension
    class ApiVerifierImpl implements ApiVerifier {
        @Override
        public void verify(ApiMethodInfo info, ApiSchema schema) {
            if (info.innerCodeMap == null) {
                return;
            }

            for (int innerCode : info.innerCodeMap.keySet()) {
                // 检测内部code是否错误的填充了当前系统的code
                if (innerCode >= info.groupInfo.minCode && innerCode < info.groupInfo.maxCode) {
                    throw new IllegalApiDefinitionException(innerCode + " is not an inner code, at " +
                        info.methodName + " when inner code mapping");
                }

                if (!schema.returnCodes.containsKey(innerCode)) {
                    throw new IllegalApiDefinitionException("undefined designed error code " +
                        innerCode + " at " + info.methodName + " when inner code mapping");
                }
            }
        }
    }

    @Extension
    class ReturnCodeTransformerImpl implements ErrorCodeFeature.ReturnCodeTransformer {
        /**
         * @param context 请求上下文
         * @param call    当前请求的API
         * @param code    待转换的错误码
         * @param next    如果无法转换，则调用next.got让其他转换器转换
         * @return 返回转换后的code
         */
        @Override
        public AbstractReturnCode transform(ApiContext context,
                                            ApiMethodCall call,
                                            AbstractReturnCode code,
                                            Next<AbstractReturnCode, RuntimeException> next) {

            int display = code.getDisplay().getCode();

            if (display <= 0 || call.method.innerReturnCodeMap == null) {
                return next.go();
            }

            AbstractReturnCode mappedCode = call.method.innerReturnCodeMap.get(display);
            if (mappedCode != null) {
                return mappedCode;
            }

            return next.go();
        }
    }
}
