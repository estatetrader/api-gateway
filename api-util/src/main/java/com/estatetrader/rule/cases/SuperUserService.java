package com.estatetrader.rule.cases;

import com.estatetrader.rule.zk.SimpleZKWatchedResource;
import com.estatetrader.util.ZKOperator;

public class SuperUserService extends SimpleZKWatchedResource {
    public SuperUserService(ZKOperator operator) {
        super("/api/super/user", operator);
    }
}
