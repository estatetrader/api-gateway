package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;
import java.util.Map;

@Description("DefaultStyleSetting")
public class DefaultStyleSetting implements ModuleSetting, Serializable {
    @Description("backgroundColor")
    public String backgroundColor;
    @Description("fontColor")
    public String fontColor;
    @Description("styles")
    public Map<String, String> styles;

    public DefaultStyleSetting(String backgroundColor, String fontColor, Map<String, String> styles) {
        this.backgroundColor = backgroundColor;
        this.fontColor = fontColor;
        this.styles = styles;
    }
}
