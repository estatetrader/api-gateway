package com.estatetrader.apigw.core.models.inject;

@FunctionalInterface
public interface DatumKeyMatcher {
    boolean matches(Object datum, Object targetKey);
}
