package com.estatetrader.apigw.core.models;

import com.estatetrader.core.ApiNotFoundException;
import com.estatetrader.document.ApiDocument;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.FileUploadInfo;

import java.util.*;

public class ApiSchema {

    public final Map<String, CommonParameterInfo> commonParameterInfoMap = new LinkedHashMap<>();
    public final Map<String, ApiMethodInfo> apiInfoMap = new LinkedHashMap<>();
    public final Map<String, ApiMethodInfo> responseInjectProviderMap = new LinkedHashMap<>();
    public final Map<String, FileUploadInfo> fileUploadInfoMap = new LinkedHashMap<>();
    public final Map<Integer, List<AbstractReturnCode>> returnCodes = new LinkedHashMap<>();
    public final ApiDocument document = new ApiDocument();

    public ApiMethodInfo renewUserTokenService;
    public ApiMethodInfo apiMockService;
    public ApiMethodInfo enabledMockedApiService;
    public ApiMethodInfo apiScribeService;
    public final Map<Integer, ApiMethodInfo> etkIssuers = new LinkedHashMap<>();

    public ApiMethodInfo getApiInfo(String methodName) {
        ApiMethodInfo info = apiInfoMap.get(methodName);
        if (info == null) {
            throw new ApiNotFoundException(methodName);
        }
        return info;
    }

    public Collection<ApiMethodInfo> getApiInfoList() {
        return apiInfoMap.values();
    }
}
