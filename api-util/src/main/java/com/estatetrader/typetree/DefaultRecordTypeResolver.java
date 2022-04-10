package com.estatetrader.typetree;

import com.estatetrader.Reflections;
import com.estatetrader.generic.GenericField;
import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.StaticType;

import java.util.Set;

public class DefaultRecordTypeResolver extends AbstractRecordTypeResolver {
    /**
     * 获取传入的泛型类型的所有可能运行时类型
     *
     * @param type 要检测的类型
     * @return 返回一个包含了所有运行时可能出现的类型的一个表达
     */
    @Override
    public GenericType concreteType(GenericType type) {
        return type.replace(new DefaultTypeConcretingReplacer());
    }

    protected boolean isRecordField(GenericField field) {
        return !field.isStatic();
    }

    /**
     * 获取传入的泛型字段的声明类型的所有可能运行时类型
     *
     * @param field 要检测的字段
     * @return 返回一个包含了所有运行时可能出现的类型的一个表达
     */
    @Override
    protected GenericType resolveFieldType(GenericField field) {
        return field.getResolvedType().replace(new DefaultTypeConcretingReplacer());
    }

    private static class DefaultTypeConcretingReplacer extends AbstractTypeConcretingReplacer {
        protected Set<Class<?>> searchSubclasses(StaticType type) {
            //noinspection unchecked
            return Reflections.current.getSubTypesOf((Class<Object>) type.getRawType());
        }

        protected boolean needConcrete(StaticType type) {
            return true;
        }
    }
}
