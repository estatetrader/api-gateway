package com.estatetrader.apigw.core.models;

import com.estatetrader.typetree.TypeSpanMetadata;

public interface TypeSpanApiMetadata extends TypeSpanMetadata {
    ApiMethodInfo getApiMethod();
}
