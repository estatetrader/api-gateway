package com.estatetrader.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.estatetrader.generic.ClassType;
import com.estatetrader.generic.GenericType;

public interface ParameterConverter {
    Object convert(String text);

    @SuppressWarnings("unchecked")
    static ParameterConverter getConverter(GenericType targetType) {
        if (targetType == null) {
            throw new NullPointerException("targetType");
        }

        if (targetType instanceof ClassType) {
            Class<?> clazz = ((ClassType) targetType).getRawType();
            if (clazz == boolean.class) {
                return Boolean::parseBoolean;
            } else if (clazz == byte.class) {
                return Byte::parseByte;
            } else if (clazz == short.class) {
                return Short::parseShort;
            } else if (clazz == int.class) {
                return Integer::parseInt;
            } else if (clazz == long.class) {
                return Long::parseLong;
            } else if (clazz == String.class) {
                return s -> s;
            } else if (clazz.isEnum()) {
                //noinspection rawtypes
                return s -> Enum.valueOf((Class<Enum>) clazz, s);
            }
        }

        // 为了防止客户端传递的非法字段干扰正常字段的解析，这里我们禁用fast json的智能匹配功能
        return s -> JSON.parseObject(s, targetType.toReflectType(), Feature.DisableFieldSmartMatch);
    }
}
