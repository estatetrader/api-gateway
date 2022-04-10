package com.estatetrader.apigw.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class AutoTypeSerializer {
    private AutoTypeSerializer() {}

    public static String serializeToString(Object object) {
        return JSON.toJSONString(object, SerializerFeature.WriteClassName);
    }

    public static <T> T deserializeFromString(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz, Feature.SupportAutoType);
    }

    public static <T> T deserializeFromString(String json, TypeReference<T> type) {
        return JSON.parseObject(json, type, Feature.SupportAutoType);
    }
}
