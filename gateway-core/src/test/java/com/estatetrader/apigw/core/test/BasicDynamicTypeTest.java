package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.*;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@ApiGroup(name = "test", minCode = 0, maxCode = 100, codeDefine = BasicDynamicTypeTest.RC.class, owner = "nick")
public class BasicDynamicTypeTest extends BaseHttpTest {
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
        @Description("mainActivity")
        @AllowedTypes({GroupBuyInfo.class, AuctionInfo.class})
        public ActivityInfo mainActivity;
        @Description("subActivities")
        public List<@AllowedTypes({GroupBuyInfo.class, AuctionInfo.class, FlashSaleInfo.class}) ? extends ActivityInfo> subActivities;
    }

    public interface ActivityInfo extends Serializable {
    }

    @Description("PriceInfo")
    @EntityGroup("Activity")
    public static class GroupBuyInfo implements ActivityInfo {
        @Description("groupSize")
        public int groupSize;
    }

    @Description("FlashSaleInfo")
    @EntityGroup("Activity")
    public static class FlashSaleInfo implements ActivityInfo {
        @Description("startTime")
        public String startTime;
    }

    @Description("AuctionInfo")
    @EntityGroup("Activity")
    public static class AuctionInfo implements ActivityInfo {
        @Description("minPrice")
        public int minPrice;
    }

    @HttpApi(name = "test.getProductInfo", desc = "getProductInfo", security = SecurityType.Anonym, owner = "nick")
    public ProductInfo getProductInfo(
            @ApiParameter(name = "pid", required = true, desc = "pid") int pid
    ) {
        ProductInfo info = new ProductInfo();
        info.productId = pid;
        info.title = "#" + pid;
        if (pid == 1) {
            GroupBuyInfo activity = new GroupBuyInfo();
            activity.groupSize = 3;
            info.mainActivity = activity;
        } else if (pid == 2) {
            AuctionInfo activity = new AuctionInfo();
            activity.minPrice = 5000;
            info.mainActivity = activity;

            GroupBuyInfo subActivity1 = new GroupBuyInfo();
            subActivity1.groupSize = 3;
            FlashSaleInfo subActivity2 = new FlashSaleInfo();
            subActivity2.startTime = "2021-10-12 10:00:00";

            info.subActivities = Arrays.asList(subActivity1, subActivity2);
        }
        return info;
    }

    @Test
    public void testGetProductInfo1() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test.getProductInfo");
        params.put("pid", "1");
        String response1 = executeRequest(params, BasicDynamicTypeTest.class);
        assertTrue(response1.contains("{\"mainActivity\":{\"groupSize\":3,\"@classKey\":\"GroupBuyInfo\"},\"productId\":1,\"title\":\"#1\"}"));
    }

    @Test
    public void testGetProductInfo2() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "test.getProductInfo");
        params.put("pid", "2");
        String response2 = executeRequest(params, BasicDynamicTypeTest.class);
        assertTrue(response2.contains("{\"mainActivity\":{\"minPrice\":5000,\"@classKey\":\"AuctionInfo\"},\"productId\":2,\"subActivities\":[{\"groupSize\":3,\"@classKey\":\"GroupBuyInfo\"},{\"startTime\":\"2021-10-12 10:00:00\",\"@classKey\":\"FlashSaleInfo\"}],\"title\":\"#2\"}"));
    }
}
