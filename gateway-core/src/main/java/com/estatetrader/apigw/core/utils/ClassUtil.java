package com.estatetrader.apigw.core.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 获取命名空间下的所有类
 *
 * @author rendong
 */
public class ClassUtil {

    /**
     * 是否是常量
     */
    public static boolean isConstField(Field field) {
        int efm = field.getModifiers();
        return Modifier.isPublic(efm) && Modifier.isStatic(efm) && Modifier.isFinal(efm);
    }
}
