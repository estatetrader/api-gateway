package com.estatetrader.apigw.load;

import com.estatetrader.apigw.core.models.ApiSchema;

/**
 * 所有网关内嵌的API服务均应实现此接口
 */
public interface ShippedService {
    /**
     * 若需要API schema，则实现此接口
     */
    interface ApiSchemaAware {
        void setApiSchema(ApiSchema apiSchema);
    }
}
