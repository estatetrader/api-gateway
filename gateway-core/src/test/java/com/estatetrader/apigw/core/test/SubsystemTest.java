package com.estatetrader.apigw.core.test;

import com.alibaba.fastjson.JSON;
import com.estatetrader.annotation.ApiGroup;
import com.estatetrader.annotation.HttpApi;
import com.estatetrader.annotation.SubsystemParamsAutowired;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.rule.zk.ItemData;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class SubsystemTest extends BaseHttpTest {
    public static class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @ApiGroup(name = "test11", minCode = 0, maxCode = 100, codeDefine = RC.class, owner = "nick")
    public static class WarehouseService {
        @HttpApi(name = "test11.geWarehouseId", desc = "test1", security = SecurityType.Anonym, owner = "nick")
        public int geWarehouseId(
            @SubsystemParamsAutowired Map<String, String> params
        ) {
            return Integer.parseInt(params.get("warehouse_id"));
        }
    }

    @Test
    public void test1() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test11.geWarehouseId");
        params.put("_scp", "{warehouse_id:1}");
        assertTrue(executeRequest(params, WarehouseService.class).contains("\"value\":1"));
    }

    @Test
    public void test2() {
        ItemData<String> item = new ItemData<>();
        String json = JSON.toJSONString(item);
        Assert.assertNotEquals("{}", json);
    }
}
