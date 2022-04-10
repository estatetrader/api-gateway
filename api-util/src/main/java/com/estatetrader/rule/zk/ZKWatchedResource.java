package com.estatetrader.rule.zk;

import com.estatetrader.util.ZKOperator;
import com.estatetrader.rule.WatchedResourceListener;
import com.estatetrader.rule.WatchedResourceManager;

public class ZKWatchedResource<T> {
    private final String rootPath;
    private final ZKOperator operator;
    private final Class<?> dataType;
    private final Class<T> childDataType;
    private Class<?>[] grandChildTypes;

    public ZKWatchedResource(String rootPath,
                             ZKOperator operator,
                             Class<T> childDataType,
                             Class<?> ... grandChildTypes) {
        this(rootPath, operator, null, childDataType, grandChildTypes);
    }

    public ZKWatchedResource(String rootPath,
                             ZKOperator operator,
                             Class<?> dataType,
                             Class<T> childDataType,
                             Class<?> ... grandChildTypes) {
        this.rootPath = rootPath;
        this.operator = operator;
        this.dataType = dataType;
        this.childDataType = childDataType;
        this.grandChildTypes = grandChildTypes;
    }

    public WatchedResourceManager<T> createManager() {
        return new ZKWatchedResourceManager<>(rootPath, operator, dataType, childDataType, grandChildTypes);
    }

    public WatchedResourceListener<T> createListener() {
        return new ZKWatchedResourceListener<>(rootPath, operator, null,
            dataType, childDataType, grandChildTypes);
    }
}
