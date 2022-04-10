package com.estatetrader.apigw.load.shipped;

import com.estatetrader.annotation.*;
import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.load.ShippedService;
import com.estatetrader.apigw.load.shipped.struct.page.*;
import com.estatetrader.define.SecurityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Extension
@ApiGroup(name = "demo", owner = "nick", codeDefine = ShippedReturnCode.class, minCode = 1000, maxCode = 1099)
public class FeatureDemoService implements ShippedService {

    @HttpApi(name = "demo.getPageData",
        security = SecurityType.Internal,
        desc = "返回页面信息")
    @DesignedErrorCode(ShippedReturnCode._C_API_DOCUMENT_GENERATION_ERROR)
    public PageData getPageData(@ApiParameter(name = "pageId", required = true, desc = "pageId") int pageId) {
        long now = System.currentTimeMillis();
        return new PageData(
            pageId,
            "#" + pageId,
            getTabModule(1),
            Arrays.asList(
                getBodyModule(2),
                getBodyModule(3)
            )
        );
    }

    @HttpApi(name = "demo.getModules",
        security = SecurityType.Internal,
        desc = "返回模块列表")
    @DesignedErrorCode(ShippedReturnCode._C_API_DOCUMENT_GENERATION_ERROR)
    public Map<Integer, Module<
                @AllowedTypes({TabSetting.class, DefaultStyleSetting.class}) ?,
                @AllowedTypes({TableEntity.class, ProductEntity.class, ArticleEntity.class}) ?
                >>
    getModules(@ApiParameter(name = "moduleIds", required = true, desc = "moduleIds") int[] moduleIds) {
        Map<Integer, Module<?, ?>> result = new LinkedHashMap<>(moduleIds.length);
        for (int moduleId : moduleIds) {
            if (moduleId == 1) {
                result.put(moduleId, getTabModule(moduleId));
            } else if (moduleId == 2 || moduleId == 3) {
                result.put(moduleId, getBodyModule(moduleId));
            }
        }
        return result;
    }

    @HttpApi(name = "demo.getBodyModuleEntity",
        security = SecurityType.Internal,
        desc = "返回模版实体")
    @DesignedErrorCode(ShippedReturnCode._C_API_DOCUMENT_GENERATION_ERROR)
    @AllowedTypes({ProductEntity.class, ArticleEntity.class})
    public ModuleEntity getBodyModuleEntity(@ApiParameter(name = "moduleId", required = true, desc = "moduleId") int moduleId) {
        if (moduleId == 1 || moduleId == 2) {
            return getProductEntity(moduleId);
        } else {
            return getArticle(moduleId);
        }
    }

    private Module<TabSetting, TableEntity> getTabModule(int moduleId) {
        if (moduleId == 1) {
            return new Module<>(
                1,
                (short) 1,
                new TabSetting("white", "black", "FIXED", "CENTER"),
                Arrays.asList(
                    new TableEntity((byte)1, "商品", Collections.singletonMap(1, new int[]{1, 2})),
                    new TableEntity((byte)2, "文章", Collections.singletonMap(2, new int[]{2, 3}))
                )
            );
        } else {
            return null;
        }
    }

    private Module<DefaultStyleSetting, ?> getBodyModule(int moduleId) {
        if (moduleId == 2) {
            return new Module<>(
                moduleId,
                (short) moduleId,
                new DefaultStyleSetting("white", "gray", Collections.singletonMap("margin", "1em")),
                Arrays.asList(getProductEntity(1), getProductEntity(2))
            );
        } else if (moduleId == 3) {
            return new Module<>(
                moduleId,
                (short) moduleId,
                new DefaultStyleSetting("white", "green", Collections.singletonMap("padding", "2em")),
                Arrays.asList(getArticle(1), getArticle(2))
            );
        } else {
            return null;
        }
    }

    private ProductEntity getProductEntity(int productId) {
        long now = System.currentTimeMillis();
        if (productId == 1) {
            return new ProductEntity(
                productId,
                "product-" + productId,
                new PreSaleInfo(1, "WARM_UP", 5000),
                new StockPrice(1, 100, true, null)
            );
        } else if (productId == 2) {
            return new ProductEntity(
                2,
                "product-2",
                new LadderGroupActivityInfo(2, now, now + 24 * 3600 * 1000),
                new StockPrice(2, 200, false,
                    Collections.singletonMap("on-sale", new ProductIndicator("on-sale", now, now + 1000))
                )
            );
        } else {
            return null;
        }
    }

    private ArticleEntity getArticle(int articleId) {
        return new ArticleEntity(articleId, (byte)1, "black", "this is an article");
    }
}
