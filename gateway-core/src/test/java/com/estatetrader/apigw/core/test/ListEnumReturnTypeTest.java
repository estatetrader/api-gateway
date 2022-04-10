package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.ApiParameter;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.annotation.ApiGroup;
import com.estatetrader.annotation.Description;
import com.estatetrader.annotation.HttpApi;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertTrue;

@ApiGroup(name = "test20", minCode = 0, maxCode = 100, codeDefine = ListEnumReturnTypeTest.RC.class, owner = "nick")
public class ListEnumReturnTypeTest extends BaseHttpTest {
    public static class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @Description("product-type")
    public enum ProductType {
        a,
        b,
        c
    }

    @HttpApi(name = "test20.getProductTypes", desc = "test1", security = SecurityType.Anonym, owner = "nick")
    public List<ProductType> getProductInfo(
        @ApiParameter(name = "pids", required = true, desc = "pids") int[] pids
    ) {
        List<ProductType> list = new ArrayList<>(pids.length);
        for (int pid : pids) {
            ProductType t;
            switch (pid) {
                case 1:
                    t = ProductType.a;
                    break;
                case 2:
                    t = ProductType.b;
                    break;
                case 3:
                    t = ProductType.c;
                    break;
                default:
                    throw new IllegalArgumentException("pid");
            }
            list.add(t);
        }
        return list;
    }

    @Test
    public void testGetProductInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test20.getProductTypes");
        params.put("pids", "[1,2,3]");
        assertTrue(executeRequest(params, ListEnumReturnTypeTest.class).contains("{\"value\":[\"a\",\"b\",\"c\"]}"));
    }
}
