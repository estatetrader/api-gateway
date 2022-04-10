package com.estatetrader.gateway.backendmsg;

@FunctionalInterface
public interface BackendMessageConditionMatcher {
    boolean match(BackendMessageCondition condition);
}
