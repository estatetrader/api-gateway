package com.estatetrader.apigw.load.shipped;

import com.estatetrader.annotation.ApiParameter;
import com.estatetrader.annotation.DesignedErrorCode;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.apigw.load.ShippedService;
import com.estatetrader.core.GatewayException;
import com.estatetrader.define.SecurityType;
import com.estatetrader.annotation.ApiGroup;
import com.estatetrader.annotation.HttpApi;
import com.estatetrader.document.ApiDocument;
import com.estatetrader.document.MethodInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Extension
@ApiGroup(name = "document", owner = "nick", codeDefine = ShippedReturnCode.class, minCode = 1000, maxCode = 1099)
public class DocumentService implements ShippedService, ShippedService.ApiSchemaAware {

    private ApiDocument document;
    private ApiDocument documentSummary;
    private Map<String, MethodInfo> methodMapping;

    @Override
    public void setApiSchema(ApiSchema apiSchema) {
        ApiDocument document = apiSchema.document;
        ApiDocument summary = new ApiDocument();
        if (document.apis != null) {
            summary.apis = new ArrayList<>(document.apis.size());
            methodMapping = new HashMap<>(document.apis.size());

            for (MethodInfo m : document.apis) {
                MethodInfo sm = new MethodInfo();

                sm.methodName = m.methodName;
                sm.origin = m.origin;
                sm.groupName = m.groupName;
                sm.groupOwner = m.groupOwner;
                sm.description = m.description;
                sm.detail = m.detail;
                sm.securityLevel = m.securityLevel;
                sm.jarFile = m.jarFile;

                summary.apis.add(sm);
                methodMapping.put(m.methodName, m);
            }
        } else {
            methodMapping = new HashMap<>();
        }

        summary.codes = document.codes;
        summary.commonParams = document.commonParams;

        this.document = document;
        this.documentSummary = summary;
    }

    @HttpApi(name = "doc.getApiDocument",
        security = SecurityType.Internal,
        desc = "获取网关API文档的基本信息（不包括API具体信息）")
    @DesignedErrorCode(ShippedReturnCode._C_API_DOCUMENT_GENERATION_ERROR)
    public ApiDocument getApiDocument() throws GatewayException {
        return document;
    }

    @HttpApi(name = "doc.getApiDocumentSummary",
        security = SecurityType.Internal,
        desc = "获取网关API文档的基本信息（不包括API具体信息）")
    @DesignedErrorCode(ShippedReturnCode._C_API_DOCUMENT_GENERATION_ERROR)
    public ApiDocument getApiDocumentSummary() throws GatewayException {
        return documentSummary;
    }

    @HttpApi(name = "doc.getApiDetailInfo",
        security = SecurityType.Internal,
        desc = "获取指定的网关API信息")
    @DesignedErrorCode({
        ShippedReturnCode._C_API_NOT_FOUND,
        ShippedReturnCode._C_API_DOCUMENT_GENERATION_ERROR
    })
    public MethodInfo getApiDetailInfo(@ApiParameter(name = "api", desc = "指定的API名称", required = true)
                                String api) throws GatewayException {
        MethodInfo info = methodMapping.get(api);
        if (info == null) {
            throw new GatewayException(ShippedReturnCode.API_NOT_FOUND);
        }
        return info;
    }
}
