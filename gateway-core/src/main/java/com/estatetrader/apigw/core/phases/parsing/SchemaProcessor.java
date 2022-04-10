package com.estatetrader.apigw.core.phases.parsing;

import com.estatetrader.apigw.core.models.ApiSchema;

public interface SchemaProcessor {
    void process(ApiSchema schema);
}
