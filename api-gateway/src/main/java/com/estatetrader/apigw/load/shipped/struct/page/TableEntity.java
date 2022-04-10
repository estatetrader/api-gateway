package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;
import java.util.Map;

@Description("TableEntity")
public class TableEntity implements ModuleEntity, Serializable {
    @Description("tag")
    public byte tag;
    @Description("displayText")
    public String displayText;
    @Description("couponProducts")
    public Map<Integer, int[]> couponProducts;

    public TableEntity(byte tag, String displayText, Map<Integer, int[]> couponProducts) {
        this.tag = tag;
        this.displayText = displayText;
        this.couponProducts = couponProducts;
    }
}
