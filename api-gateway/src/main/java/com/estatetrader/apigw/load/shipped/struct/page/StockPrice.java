package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;
import java.util.Map;

@Description("StockPrice")
public class StockPrice implements Serializable {
    @Description("id")
    public int id;
    @Description("stock")
    public int stock;
    @Description("onSale")
    public boolean onSale;
    @Description("indicators")
    public Map<String, ProductIndicator> indicators;

    public StockPrice(int id, int stock, boolean onSale, Map<String, ProductIndicator> indicators) {
        this.id = id;
        this.stock = stock;
        this.onSale = onSale;
        this.indicators = indicators;
    }
}
