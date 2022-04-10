package com.estatetrader.apigw.load.shipped.struct.page;

import com.estatetrader.annotation.Description;

import java.io.Serializable;

@Description("ArticleEntity")
public class ArticleEntity implements ModuleEntity, Serializable {
    @Description("articleId")
    public int articleId;
    @Description("code")
    public byte code;
    @Description("color")
    public String color;
    @Description("content")
    public String content;

    public ArticleEntity(int articleId, byte code, String color, String content) {
        this.articleId = articleId;
        this.code = code;
        this.color = color;
        this.content = content;
    }
}
