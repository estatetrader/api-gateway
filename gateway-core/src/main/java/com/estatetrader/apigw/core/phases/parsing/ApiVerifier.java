package com.estatetrader.apigw.core.phases.parsing;

import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.models.ApiSchema;

public interface ApiVerifier {
    void verify(ApiMethodInfo info, ApiSchema schema);

    default void verify(ApiSchema schema) {
        for (ApiMethodInfo info : schema.getApiInfoList()) {
            try {
                verify(info, schema);
            } catch (IllegalApiDefinitionException e) {
                throw new IllegalApiDefinitionException(String.format(
                    "verify API method info %s (owner %s) failed in jar %s: %s",
                    info.methodName, info.owner, info.jarFileSimpleName, e.getMessage()), e);
            }
        }
    }
}
