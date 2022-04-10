package com.estatetrader.apigw.core.test;

import com.alibaba.fastjson.JSON;
import com.estatetrader.annotation.*;
import com.estatetrader.apigw.core.test.support.TestDubboSupport;
import com.estatetrader.entity.ServiceRuntimeException;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.MockApiConfigInfo;
import com.estatetrader.define.ResponseFilter;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ApiReturnCode;
import com.estatetrader.apigw.core.features.MockServiceFeature;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertTrue;

@ApiGroup(name = "test12", minCode = 0, maxCode = 100, codeDefine = ApiMockServiceTest.RC.class, owner = "nick")
public class ApiMockServiceTest extends BaseHttpTest implements TestDubboSupport {
    public static class RC extends AbstractReturnCode {

        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @Description("product info")
    public static class ProductInfo implements Serializable {
        @Description("product-id")
        public int productId;
        @Description("price")
        public int price;
    }

    @Description("getEnabledMockedApis parameter")
    public static class EnabledMockedApisParameters implements Serializable {
        @Description("requestApis")
        public List<String> requestApis;
        @Description("deviceId")
        public long deviceId;
        @Description("appId")
        public int appId;
        @Description("version")
        public String version;
    }

    @HttpApi(name = "test12.getProductInfo", desc = "test1", security = SecurityType.Anonym, owner = "nick")
    @FilterResponse(type = ProductResponseFilter.class)
    public ProductInfo getProductInfo(
            @ApiParameter(name = "pid", required = true, desc = "pid") int pid,
            @ApiAutowired(CommonParameter.versionCode) int vc
    ) {
        ProductInfo info = new ProductInfo();
        info.productId = pid;
        info.price = 20;
        return info;
    }

    public static class ProductResponseFilter implements ResponseFilter {
        /**
         * 将在被拦截的API返回后执行，在该函数内部提供
         *
         * @param response 被拦截的接口的返回值
         */
        @Override
        public void filter(Object response) {
            // do not enter this method
            throw new IllegalStateException();
        }
    }

    @EnabledMockedApiService
    @HttpApi(name = "test12.getEnabledMockedApis", desc = "test1", security = SecurityType.Anonym, owner = "nick")
    public MockApiConfigInfo getEnabledMockedApis(
        @ApiParameter(required = true, name = "parameters", desc = "parameters") EnabledMockedApisParameters parameters) {

        if (parameters.deviceId == 1) {
            List<String> allMocked = new ArrayList<>(Arrays.asList("test12.getProductInfo", "test12.getProductInfoV2"));
            List<String> result = new ArrayList<>();
            for (String api : parameters.requestApis) {
                if (allMocked.contains(api)) {
                    result.add(api);
                }
            }
            MockApiConfigInfo info = new MockApiConfigInfo();
            info.apisToMock = result;
            return info;
        }
        return new MockApiConfigInfo();
    }

    @ApiMockService
    @HttpApi(name = "test12.mockProvider", desc = "test1", security = SecurityType.Anonym, owner = "nick")
    public String mockProvider(
        @ApiParameter(required = true, name = "mockedApi", desc = "requestApis") String mockedApi,
        @ApiParameter(required = true, name = "apiParameters", desc = "apiParameters") String apiParameters,
        @ApiAutowired(CommonParameter.applicationId) int aid) {

        Map parameters = JSON.parseObject(apiParameters);
        if ("test12.getProductInfo".equals(mockedApi) && aid == 1) {
            ProductInfo p = new ProductInfo();
            p.productId = (int) parameters.get("pid");
            p.price = 21;
            return asyncReturnDubbo(JSON.toJSONString(p), 1000);
        } else {
            throw new ServiceRuntimeException(ApiReturnCode.MOCK_DATA_NOT_FOUND, "mock not found");
        }
    }

    @Test
    public void testGetProductInfoSpecifiedByBackend() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test12.getProductInfo");
        params.put("_aid", "1");
        params.put("pid", "1");
        params.put("_did", "1");
        getBean(MockServiceFeature.Config.class).setFeatureEnabled(true);
        assertTrue(executeRequest(params, ApiMockServiceTest.class).contains("\"price\":21"));
    }

    @Test
    public void testNotFound() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test12.getProductInfo");
        params.put("_aid", "2");
        params.put("pid", "1");
        params.put("_did", "1");
        getBean(MockServiceFeature.Config.class).setFeatureEnabled(true);
        assertTrue(executeRequest(params, ApiMockServiceTest.class).contains("mock not found"));
    }
}
