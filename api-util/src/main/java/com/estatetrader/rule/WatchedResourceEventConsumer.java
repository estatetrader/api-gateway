package com.estatetrader.rule;

import java.util.List;

public interface WatchedResourceEventConsumer {
    void onChildrenChange(List<String> oldChildren, List<String> newChildren);

    void onChildDataChange(String key, Object oldValue, Object newValue);
}
