package com.estatetrader.rule.definition;

import java.util.*;

public interface ApiDefinitionManager {
    void sync(Collection<ApiInfo> apiList);
    ApiInfo get(String apiName);
}
