package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.*;
import com.estatetrader.define.SecurityType;
import com.estatetrader.define.ServiceInjectable;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.responseEntity.StringResp;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * Created by nick on 22/08/2017.
 */
@ApiGroup(name = "test7", minCode = 0, maxCode = 100, codeDefine = ServiceInjectionTest.RC.class, owner = "nick")
public class ServiceInjectionTest extends BaseHttpTest {

    public static class RC extends AbstractReturnCode {

        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @HttpApi(name = "test7.t1", desc = "test1", security = SecurityType.Anonym, owner = "nick")
    @ExportParam(name = "user.ids")
    public StringResp t1(
            @ApiParameter(name = "id", required = true, desc = "id")
            int id
    ) {
        StringResp s = new StringResp();
        s.value = String.valueOf("t1=" + id);
        ServiceInjectable.export("user.ids", id + 1);
        return s;
    }

    @HttpApi(name = "test7.t2", desc = "test2", security = SecurityType.Anonym, owner = "nick")
    @ExportParam(name = "product.ids")
    @ExportParam(name = "order.ids")
    public StringResp t2(
            @ImportParam("user.ids")
            @ApiParameter(name = "userIds", desc = "user ids", required = false)
            String userIds
    ) {
        StringResp s = new StringResp();
        s.value = "t2=" + userIds;
        ServiceInjectable.export("product.ids", Integer.parseInt(userIds) + 100);
        ServiceInjectable.export("order.ids", Integer.parseInt(userIds) + 200);
        return s;
    }

    @HttpApi(name = "test7.t3", desc = "test3", security = SecurityType.Anonym, owner = "nick")
    public StringResp t3(
            @ImportParam("user.ids")
            @ApiParameter(name = "userIds", desc = "user ids", required = false)
                    String userIds,
            @ImportParam("product.ids")
            @ApiParameter(name = "productIds", desc = "product ids", required = false)
                    String productIds,
            @ImportParam("order.ids")
            @ApiParameter(name = "orderIds", desc = "order ids", required = false)
                    String orderIds
    ) {
        StringResp s = new StringResp();
        s.value = "t3=" + userIds + "," + productIds + "," + orderIds;
        return s;
    }

    @HttpApi(name = "test7.t4", desc = "test4", security = SecurityType.Anonym, owner = "nick")
    public StringResp t4(
            @ImportParam("order.ids")
            @ApiParameter(name = "orderIds", desc = "order ids", required = false)
                    String orderIds
    ) {
        StringResp s = new StringResp();
        s.value = "t4=" + orderIds;
        return s;
    }

    @Test
    public void test() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test7.t1,test7.t2:test7.t1,test7.t3:test7.t1/test7.t2,test7.t4:test7.t2");
        params.put("0_id", "1");
        String result = executeRequest(params, ServiceInjectionTest.class);
        assertTrue(result.contains("t2=2"));
        assertTrue(result.contains("t3=2,102,202"));
        assertTrue(result.contains("t4=202"));
    }
}
