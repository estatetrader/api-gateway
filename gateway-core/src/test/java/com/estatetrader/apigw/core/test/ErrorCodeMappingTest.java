package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.ApiGroup;
import com.estatetrader.annotation.DesignedErrorCode;
import com.estatetrader.annotation.ErrorCodeMapping;
import com.estatetrader.annotation.HttpApi;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.entity.ServiceRuntimeException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class ErrorCodeMappingTest extends BaseHttpTest {

    @ApiGroup(name = "test8", minCode = 1, maxCode = 2, codeDefine = Test1.RC.class, owner = "nick")
    public static class Test1 {
        public static class RC extends AbstractReturnCode {
            protected RC(String desc, int code) {
                super(desc, code);
            }

            public static final AbstractReturnCode CODE1 = new Test1.RC("code1", 1);
        }

        @ErrorCodeMapping(mapping = {1, 2})
        @DesignedErrorCode({1})
        @HttpApi(name = "test8.t1", desc = "test1", security = SecurityType.Anonym, owner = "nick")
        public String t1() {
            return new Test2().t2();
        }
    }

    @ApiGroup(name = "test9", minCode = 2, maxCode = 3, codeDefine = Test2.RC.class, owner = "nick")
    public static class Test2 {
        public static class RC extends AbstractReturnCode {
            protected RC(String desc, int code) {
                super(desc, code);
            }

            public static final AbstractReturnCode CODE2 = new RC("code2", 2);
        }

        @DesignedErrorCode({2})
        @HttpApi(name = "test9.t2", desc = "test2", security = SecurityType.Anonym, owner = "nick")
        public String t2() {
            throw new ServiceRuntimeException(RC.CODE2);
        }
    }

    @Test
    public void testGetProductInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test8.t1");
        String result = executeRequest(params, Test1.class, Test2.class);
        assertTrue(result.contains("\"code\":1"));
    }
}
