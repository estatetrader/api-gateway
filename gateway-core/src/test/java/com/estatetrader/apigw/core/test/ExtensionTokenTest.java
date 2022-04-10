package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.*;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by nick on 22/08/2017.
 */
public class ExtensionTokenTest extends BaseHttpTest {

    public static class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @EntityGroup("product")
    @Description("product-info")
    public static class ProductInfo implements Serializable {
        @Description("product.id")
        public int id;
        @Description("name")
        public String name;
        @Description("warehouse id")
        public String warehouseId;
    }

    @ApiGroup(name = "test10", minCode = 0, maxCode = 100, codeDefine = RC.class, owner = "nick")
    public static class ProductService {
        @HttpApi(name = "test10.getProduct", desc = "test1", security = SecurityType.RegisteredDevice, owner = "nick")
        public ProductInfo geProduct(
            @ApiParameter(name = "id", required = true, desc = "id")
                int id,
            @ExtensionParamsAutowired Map<String, String> etkParams
        ) {
            ProductInfo info = new ProductInfo();
            info.id = id;
            info.name = "product#" + id;
            info.warehouseId = etkParams.get("warehouse_id");
            return info;
        }
    }

    @Test
    public void test1() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test10.getProduct");
        params.put("_tk", "dtk_3saoX3A04pl8OmG9tt0uodzghh43tCN5GQOOE7krsB8AsIWlHQpTeyM9X/KC49d5jKep6uK7H/T2v1/UBAwKcuctfdX44gRADNvl65fw1O561jsxII0S6+VaHmYdTwaU");
        params.put("id", "1");
        params.put("_aid", "16");
        assertTrue(executeRequest(params, ProductService.class).contains("\"name\":\"product#1\""));
    }
}
