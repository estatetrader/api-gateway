package com.estatetrader.apigw.core.test;

import com.estatetrader.annotation.*;
import com.estatetrader.annotation.inject.*;
import com.estatetrader.common.entity.BaseEntity;
import com.estatetrader.define.CommonParameter;
import com.estatetrader.define.Datum;
import com.estatetrader.define.SecurityType;
import com.estatetrader.entity.AbstractReturnCode;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

@ApiGroup(name = "test", minCode = 0, maxCode = 100, codeDefine = ApiResponseInjectTest.RC.class, owner = "nick")
public class ApiResponseInjectTest extends BaseHttpTest {
    public static class RC extends AbstractReturnCode {
        protected RC(String desc, int code) {
            super(desc, code);
        }
    }

    @Description("Simple KLine")
    public static class SimpleArticleList extends BaseEntity {

        @Description("文章ID")
        @ExportDatumKey(keyName = "articleId", datumType = "ArticleInfo")
        public List<Long> articleIds = new ArrayList<>();

        @Description("文章信息列表")
        @InjectDatum(datumType = "ArticleInfo")
        public List<Datum> articleList;

        @Description("has more")
        @ExampleValue("true")
        public boolean hasMore;

        @Description("nextOffset")
        @ExampleValue("nextOffset")
        public int nextOffset;
    }

    @Description("文章的简略信息，不包含文章正文")
    @DefineDatum("ArticleInfo")
    public static class ArticleInfo extends BaseEntity {
        @Description("文章ID")
        @DatumKey("articleId")
        public Integer id;

        @Description("作者用户id")
        public Long authorId;

        @Description("内容标题")
        public String title;

        @Description("摘要")
        public String summary;

        @Description("封面图链接")
        public String coverFigure;

        @Description("创建时间")
        public Long createTime;

        @Description("最后更新时间")
        public Long lastUpdateTime;
    }

    @HttpApi(
        name = "article.getArticles",
        desc = "根据文章id获取文章的简略信息",
        security = SecurityType.Anonym,
        owner = "nick")
    @ResponseInjectProvider("article.getArticles")
    public List<ArticleInfo> getArticles(
        @ImportDatumKey(keyName = "articleId")
        @ApiParameter(name = "articleIds", required = true, desc = "articleIds") List<Integer> articleIds
    ) {
        return articleIds.stream()
            .map(id -> {
                ArticleInfo info = new ArticleInfo();
                info.id = id;
                info.authorId = 3L;
                return info;
            }).collect(Collectors.toList());
    }

    @HttpApi(
        name = "articleRetrieval.discoverArticles",
        desc = "discover articles",
        security = SecurityType.Anonym,
        owner = "sword865")
    @ResponseInjectFromApi("article.getArticles")
    public SimpleArticleList discoverArticles(
        @ApiAutowired(CommonParameter.deviceId) long deviceId,
        @ApiAutowired(CommonParameter.userId) long userId,
        @ApiAutowired(CommonParameter.applicationId) int appId,
        @ApiParameter(name = "sessionId", required = true, desc = "session") String session,
        @ApiParameter(name = "limit", required = true, desc = "limit") int limit,
        @ApiParameter(name = "offset", required = false,  defaultValue = "-1", desc = "offset") int offset
    ) {
        SimpleArticleList result = new SimpleArticleList();
        result.articleIds = Arrays.asList(1L, 2L, 3L);
        return result;
    }

    @Test
    public void testGetProductInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("_mt", "articleRetrieval.discoverArticles");
        params.put("sessionId", "123");
        params.put("limit", "100");
        String result = executeRequest(params, ApiResponseInjectTest.class);
        assertTrue(result.contains("\"content\":[{\"articleIds\":[1,2,3],\"articleList\":[{\"authorId\":3,\"coverFigure\":null,\"createTime\":null,\"id\":1,\"lastUpdateTime\":null,\"summary\":null,\"title\":null},{\"authorId\":3,\"coverFigure\":null,\"createTime\":null,\"id\":2,\"lastUpdateTime\":null,\"summary\":null,\"title\":null},{\"authorId\":3,\"coverFigure\":null,\"createTime\":null,\"id\":3,\"lastUpdateTime\":null,\"summary\":null,\"title\":null}],\"hasMore\":false,\"nextOffset\":0}"));
    }
}
