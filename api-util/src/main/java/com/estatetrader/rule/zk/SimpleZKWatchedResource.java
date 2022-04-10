package com.estatetrader.rule.zk;

import com.estatetrader.util.ZKOperator;

public class SimpleZKWatchedResource extends ZKWatchedResource<Void> {
    public SimpleZKWatchedResource(String rootPath, ZKOperator operator) {
        super(rootPath, operator, null);
    }
}
