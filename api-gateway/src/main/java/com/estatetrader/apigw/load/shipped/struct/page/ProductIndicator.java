package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("ProductIndicator")
public class ProductIndicator implements Serializable {
    @Description("label")
    public String label;
    @Description("startTime")
    public long startTime;
    @Description("endTime")
    public long endTime;

    public ProductIndicator(String label, long startTime, long endTime) {
        this.label = label;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
