package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.*;
import com.estatetrader.define.SecurityType;
import com.estatetrader.document.GenericTypeInfo;
import com.estatetrader.document.TypeStruct;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.document.FieldInfo;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@ApiGroup(name = "test", minCode = 0, maxCode = 100, codeDefine = BasicApiTest.RC.class, owner = "nick")
public class BasicApiTest extends BaseHttpTest {
    public static class RC extends AbstractReturnCode {

        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @Description("product info")
    @EntityGroup("Product")
    public static class ProductInfo implements Serializable {
        @Description("product-id")
        public long productId;
        @Description("title")
        public String title;
        @Description("price")
        public PriceInfo price;
    }

    @Description("PriceInfo")
    @EntityGroup("Price")
    public static class PriceInfo implements Serializable {
        @Description("maxPrice")
        public Integer maxPrice;
        @Description("minPrice")
        public Integer minPrice;
    }

    @HttpApi(name = "test.getProductInfo", desc = "test", security = SecurityType.Anonym, owner = "nick")
    public ProductInfo getProductInfo(
            @ApiParameter(name = "pid", required = true, desc = "pid") Integer pid
    ) {
        ProductInfo info = new ProductInfo();
        info.productId = pid;
        info.title = "#" + pid;
        info.price = new PriceInfo();
        info.price.maxPrice = 500;
        info.price.minPrice = 300;
        return info;
    }

    @HttpApi(name = "test.getProductInfoList", desc = "test", security = SecurityType.Anonym, owner = "nick")
    public List<ProductInfo> getProductInfoList(
        @ApiParameter(name = "pids", required = true, desc = "pids") int[] pids
    ) {
        List<ProductInfo> list = new ArrayList<>(pids.length);
        for (int pid : pids) {
            ProductInfo info = new ProductInfo();
            info.productId = pid;
            info.title = "#" + pid;
            info.price = new PriceInfo();
            info.price.maxPrice = 500;
            info.price.minPrice = 300;
            list.add(info);
        }

        return list;
    }

    @HttpApi(name = "test.voidReturn", desc = "test", security = SecurityType.Anonym, owner = "nick")
    public void voidReturn() {
    }

    @Test
    public void testGetProductInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test.getProductInfo");
        params.put("pid", "1");
        String response = executeRequest(params, BasicApiTest.class);
        assertTrue(response.contains("{\"price\":{\"maxPrice\":500,\"minPrice\":300},\"productId\":1,\"title\":\"#1\"}"));
    }

    @Test
    public void testGetProductInfoList() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test.getProductInfoList");
        params.put("pids", "[1,2,3]");
        String response = executeRequest(params, BasicApiTest.class);
        assertTrue(response.contains("{\"price\":{\"maxPrice\":500,\"minPrice\":300},\"productId\":1,\"title\":\"#1\"}"));
    }

    @Test
    public void testParseApi() {
        ApiSchema schema = parseApi(BasicApiTest.class);
        Assert.assertEquals(7, schema.document.structures.size());
        // Api_ObjectArrayResp
        {
            TypeStruct struct = schema.document.findTypeStruct("Api_ObjectArrayResp");
            Assert.assertEquals(1, struct.fields.size());
            FieldInfo field = struct.fields.get(0);
            Assert.assertEquals("value", field.name);
            Assert.assertEquals(GenericTypeInfo.listType(GenericTypeInfo.typeVariable("T")), field.type);
            Assert.assertEquals(1, struct.typeVars.size());
            Assert.assertEquals("T", struct.typeVars.get(0));
        }
        // ProductInfo
        {
            TypeStruct struct = schema.document.findTypeStruct("Api_PRODUCT_BasicApiTest_ProductInfo");
            Assert.assertEquals(3, struct.fields.size());
            Assert.assertEquals(GenericTypeInfo.normalType("long"), struct.findField("productId").type);
            Assert.assertEquals(GenericTypeInfo.normalType("string"), struct.findField("title").type);
            Assert.assertEquals(GenericTypeInfo.normalType("Api_PRICE_BasicApiTest_PriceInfo"), struct.findField("price").type);
            Assert.assertNull(struct.typeVars);
        }
        // PriceInfo
        {
            TypeStruct struct = schema.document.findTypeStruct("Api_PRICE_BasicApiTest_PriceInfo");
            Assert.assertEquals(2, struct.fields.size());
            Assert.assertEquals(GenericTypeInfo.normalType("int"), struct.findField("maxPrice").type);
            Assert.assertEquals(GenericTypeInfo.normalType("int"), struct.findField("minPrice").type);
            Assert.assertNull(struct.typeVars);
        }

        Assert.assertEquals(3, schema.apiInfoMap.size());
        Assert.assertEquals(3, schema.document.apis.size());
        // test.getProductInfo
        {
            GenericTypeInfo returnType = schema.document.findMethod("test.getProductInfo").returnType;
            Assert.assertEquals(GenericTypeInfo.normalType("Api_PRODUCT_BasicApiTest_ProductInfo"), returnType);
        }
        // test.getProductInfoList
        {
            GenericTypeInfo returnType = schema.document.findMethod("test.getProductInfoList").returnType;
            GenericTypeInfo expectedType = GenericTypeInfo.parameterizedType(
                "Api_ObjectArrayResp",
                GenericTypeInfo.normalType("Api_PRODUCT_BasicApiTest_ProductInfo")
            );
            Assert.assertEquals(expectedType, returnType);
        }
    }
}
