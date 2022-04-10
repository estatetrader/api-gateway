package com.estatetrader.objtree;

import com.estatetrader.objtree.ObjectTree;
import com.estatetrader.objtree.ObjectTreeBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ObjectTreeTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ProductId {}

    public static class Page {
        public Head head;
        public List<Paragraph> paragraphs;
        public Tail tail;
    }

    public static class Head {
        public Product product;
    }

    public static class Tail {
        public BriefProduct product;
    }

    public static class Paragraph {
        public Product product;
    }

    public static class Product {
        @ProductId
        public int productId;
        public String detail;
    }

    public static class BriefProduct {
        @ProductId
        public int id;
    }

    private Page createPage() {
        Page page = new Page();
        Head head = new Head();
        head.product = new Product();
        head.product.productId = 1;
        head.product.detail = "#1";
        page.head = head;

        page.paragraphs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Paragraph p = new Paragraph();
            p.product = new Product();
            p.product.productId = i + 2;
            p.product.detail = "#" + (i + 2);
            page.paragraphs.add(p);
        }

        Tail tail = new Tail();
        tail.product = new BriefProduct();
        tail.product.id = 12;
        page.tail = tail;
        return page;
    }

    @Test
    public void testUsingParam() {
        abstract class ProductCollector {
            abstract void collect(Product product);
        }

        ObjectTree<ProductCollector> visitor = new ObjectTreeBuilder
            <ProductCollector>(Page.class)
            .adviceRecord(Product.class, (v, p, c) -> p.collect(v))
            .build();

        Page page = createPage();

        class ProductIdCollector extends ProductCollector {
            final List<Integer> productIds = new ArrayList<>();

            @Override
            public void collect(Product product) {
                productIds.add(product.productId);
            }
        }

        ProductIdCollector idCollector = new ProductIdCollector();
        visitor.visit(page, idCollector);
        Assert.assertEquals(
            IntStream.range(1, 12).boxed().collect(Collectors.toList()),
            idCollector.productIds
        );

        class ProductDetailCollector extends ProductCollector {
            final StringBuilder sb = new StringBuilder();

            @Override
            public void collect(Product product) {
                sb.append(product.detail).append("\n");
            }
        }

        ProductDetailCollector detailCollector = new ProductDetailCollector();
        visitor.visit(page, detailCollector);
        Assert.assertEquals(
            IntStream.range(1, 12).mapToObj(i -> "#" + i + "\n").collect(Collectors.joining()),
            detailCollector.sb.toString()
        );
    }

    @Test
    public void testByType() {
        class ProductIdCollector {
            final List<Integer> productIds = new ArrayList<>();
        }

        ObjectTree<ProductIdCollector> visitor = new ObjectTreeBuilder
            <ProductIdCollector>(Page.class)
            .adviceRecord(Product.class, (v, p, c) -> p.productIds.add(v.productId))
            .adviceRecord(BriefProduct.class, (v, p, c) -> p.productIds.add(v.id))
            .build();

        Page page = createPage();

        ProductIdCollector idCollector = new ProductIdCollector();
        visitor.visit(page, idCollector);
        Assert.assertEquals(
            IntStream.range(1, 13).boxed().collect(Collectors.toList()),
            idCollector.productIds
        );
    }

    @Test
    public void testByAnnotation() {
        class ProductIdCollector {
            final List<Integer> productIds = new ArrayList<>();
        }

        ObjectTree<ProductIdCollector> visitor = new ObjectTreeBuilder
            <ProductIdCollector>(Page.class)
            .adviceAnnotatedField(ProductId.class, (v, p, c) -> p.productIds.add((Integer) v))
            .build();

        Page page = createPage();

        ProductIdCollector idCollector = new ProductIdCollector();
        visitor.visit(page, idCollector);
        Assert.assertEquals(
            IntStream.range(1, 13).boxed().collect(Collectors.toList()),
            idCollector.productIds
        );
    }
}
