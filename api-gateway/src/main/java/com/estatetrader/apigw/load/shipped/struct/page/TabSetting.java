package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("TabSetting")
public class TabSetting implements ModuleSetting, Serializable {
    @Description("backgroundColor")
    public String backgroundColor;
    @Description("foreColor")
    public String foreColor;
    @Description("showType")
    public String showType;
    @Description("alignType")
    public String alignType;

    public TabSetting(String backgroundColor, String foreColor, String showType, String alignType) {
        this.backgroundColor = backgroundColor;
        this.foreColor = foreColor;
        this.showType = showType;
        this.alignType = alignType;
    }
}
