package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.AllowedTypes;
import com.estatetrader.annotation.Description;

import java.io.Serializable;
import java.util.List;

@Description("PageData")
public class PageData implements Serializable {
    @Description("pageId")
    public int pageId;
    @Description("title")
    public String title;
    @Description("tabs")
    public Module<TabSetting, TableEntity> tabs;
    @Description("body")
    @AllowedTypes({ProductEntity.class, ArticleEntity.class})
    public List<Module<DefaultStyleSetting, ?>> body;

    public PageData(int pageId, String title, Module<TabSetting, TableEntity> tabs, List<Module<DefaultStyleSetting, ?>> body) {
        this.pageId = pageId;
        this.title = title;
        this.tabs = tabs;
        this.body = body;
    }
}
