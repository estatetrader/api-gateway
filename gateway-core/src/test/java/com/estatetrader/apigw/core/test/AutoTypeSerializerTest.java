package com.estatetrader.apigw.core.test;

import com.estatetrader.apigw.core.utils.AutoTypeSerializer;
import org.junit.Assert;
import org.junit.Test;

public class AutoTypeSerializerTest {
    @Test
    public void testSerialize() {
        A a = new A();
        B b = new B();
        b.n = 3;
        a.b = b;
        String json = AutoTypeSerializer.serializeToString(a);
        Assert.assertEquals("{" +
            "\"@type\":\"com.estatetrader.apigw.core.test.AutoTypeSerializerTest$A\"," +
            "\"b\":{\"@type\":\"com.estatetrader.apigw.core.test.AutoTypeSerializerTest$B\",\"n\":3}" +
            "}", json);
    }

    @Test
    public void testDeserialize() {
        A a = new A();
        B b = new B();
        b.n = 3;
        a.b = b;
        String json = AutoTypeSerializer.serializeToString(a);
        A a2 = AutoTypeSerializer.deserializeFromString(json, A.class);
        Assert.assertEquals(B.class, a2.b.getClass());
    }

    public static class A {
        public Object b;
    }

    public static class B {
        public int n;
        public Object c;
    }

    public static class C {
        public int m;
    }
}
