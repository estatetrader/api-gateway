package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("PreSaleInfo")
public class PreSaleInfo implements ActivityInfo, Serializable {
    @Description("activityId")
    public int activityId;
    @Description("activityStatus")
    public String activityStatus;
    @Description("price")
    public int price;

    public PreSaleInfo(int activityId, String activityStatus, int price) {
        this.activityId = activityId;
        this.activityStatus = activityStatus;
        this.price = price;
    }
}
