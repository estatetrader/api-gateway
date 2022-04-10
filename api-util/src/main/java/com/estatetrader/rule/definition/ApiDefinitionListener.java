package com.estatetrader.rule.definition;

import java.util.List;
import java.util.Set;

public interface ApiDefinitionListener {
    List<ApiInfo> getApiInfoList();

    Set<String> subsystemApiList(String subsystem);

    boolean subsystemContainsApi(String subsystem, String apiName);

    String getSubsystemNameFromApi(String api);

    ApiInfo getApiInfo(String apiName);
}
