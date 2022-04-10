package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.*;
import com.estatetrader.annotation.inject.*;
import com.estatetrader.define.Datum;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import com.estatetrader.common.entity.BaseEntity;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

@ApiGroup(name = "test", minCode = 0, maxCode = 100, codeDefine = DatumInjectWithoutDatumKeyTest.RC.class, owner = "nick")
public class DatumInjectWithoutDatumKeyTest extends BaseHttpTest {
    public static class RC extends AbstractReturnCode {

        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @HttpApi(name = "vipSuperCardActivity.queryUserEffectiveVipSuperCardActivity", desc = "查询用户当前有效的VIP大牌卡购卡返劵活动", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectFromApi(value = "coupon.batchGetCoupon")
    public VipSuperCardUserActivityInfo queryUserEffectiveVipSuperCardActivity() {
        VipSuperCardUserActivityInfo activityInfo = new VipSuperCardUserActivityInfo();
        activityInfo.couponCodeList = Arrays.asList("123", "456", "789");
        return activityInfo;
    }

    @HttpApi(name = "coupon.batchGetCouponList", desc = "根据couponCode查询优惠券信息", security = SecurityType.Anonym, owner = "nick")
    @ResponseInjectProvider("coupon.batchGetCoupon")
    public List<CouponInfo> batchGetCouponList(
        @ApiCookieAutowired({"utm_source"}) Map<String, String> cookieMap,
        @ApiParameter(required = true, name = "couponCodes", desc = "优惠券code列表")
        @ImportDatumKey(keyName = "couponCodes") String[] couponCodes) {
        return Arrays.stream(couponCodes).map(c -> {
            switch (c) {
                case "123":
                    return new CouponInfo("123", "#123");
                case "456":
                    return new CouponInfo("456", "#456");
                case "789":
                    return new CouponInfo("789", "#789");
                default:
                    return null;
            }
        }).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Description("vip大牌卡用户活动信息")
    public static class VipSuperCardUserActivityInfo extends BaseEntity {
        @Description("优惠劵code")
        @ExportDatumKey(keyName = "couponCodes", datumType = "couponInfo")
        public List<String> couponCodeList;

        @Description("活动赠送的优惠劵信息")
        @InjectDatum(datumType = "couponInfo")
        public List<Datum> couponList;
    }

    @Description("优惠券信息")
    @DefineDatum("couponInfo")
    public static class CouponInfo implements Datum {
        @Description("券码")
        @DatumKey("code")
        public String code;

        @Description("名称（平台券与originalName一致，店铺券与具体的适用商品有关，系统自动生成）")
        public String name;

        public CouponInfo(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Test
    public void testGetProductInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "vipSuperCardActivity.queryUserEffectiveVipSuperCardActivity");
        String result = executeRequest(params, DatumInjectWithoutDatumKeyTest.class);
        assertTrue(result.contains("\"content\":[{\"couponCodeList\":[\"123\",\"456\",\"789\"],\"couponList\":[{\"code\":\"123\",\"name\":\"#123\"},{\"code\":\"456\",\"name\":\"#456\"},{\"code\":\"789\",\"name\":\"#789\"}]}]"));
    }
}
