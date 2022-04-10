package com.estatetrader.apigw.core.phases.parsing;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.define.ApiOpenState;
import com.estatetrader.define.IllegalApiDefinitionException;

import java.util.List;

public interface ApiRegister {

    void register(ApiMethodInfo info, ApiSchema schema);

    default void register(List<ApiMethodInfo> infoList, ApiSchema schema) {
        for (ApiMethodInfo info : infoList) {
            try {
                register(info, schema);
            } catch (IllegalApiDefinitionException e) {
                throw new IllegalApiDefinitionException(String.format(
                    "register API method info %s (owner %s) failed in jar %s: %s",
                    info.methodName,
                    info.owner,
                    info.jarFileSimpleName,
                    e.getMessage()),
                    e);
            }
        }
    }

    @Extension(first = true)
    class ApiInfoToSchemaRegister implements ApiRegister {

        @Override
        public void register(ApiMethodInfo info, ApiSchema schema) {
            ApiMethodInfo previous = schema.apiInfoMap.get(info.methodName);
            if (previous != null) {
                throw new IllegalApiDefinitionException(
                    String.format(
                        "duplicate definition for API %s, which is already defined in interface in jar %s",
                        info.methodName,
                        info.jarFileSimpleName
                    )
                );
            }

            if (info.state == ApiOpenState.DOCUMENT || //只需要生成文档的api，不创建代理
                info.state == ApiOpenState.OPEN ||
                info.state == ApiOpenState.DEPRECATED) {

                schema.apiInfoMap.put(info.methodName, info);
            }
        }
    }
}
