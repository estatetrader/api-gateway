package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.AllowedTypes;
import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("ProductEntity")
public class ProductEntity implements ModuleEntity, Serializable {
    @Description("id")
    public int id;
    @Description("productName")
    public String productName;
    @Description("activityInfo")
    @AllowedTypes({PreSaleInfo.class, LadderGroupActivityInfo.class})
    public ActivityInfo activityInfo;
    @Description("stockPrice")
    public StockPrice stockPrice;

    public ProductEntity(int id, String productName, ActivityInfo activityInfo, StockPrice stockPrice) {
        this.id = id;
        this.productName = productName;
        this.activityInfo = activityInfo;
        this.stockPrice = stockPrice;
    }
}
