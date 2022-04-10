package com.estatetrader.typetree;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractRecordTypeResolver implements RecordTypeResolver {
    /**
     * 判断指定的类型是否为记录类型，记录类型指的是除简单类型、数组、集合、哈希表、注解、接口以及少数特殊的保留类型外的所有具体类类型
     *
     * @param type 要检测的类型
     * @return 返回true表示指定的type是记录类型
     */
    @Override
    public boolean isRecordType(StaticType type) {
        Class<?> clazz = type.getRawType();
        return !(type instanceof ClassType && ((ClassType) type).isSimpleType())
            && !(type instanceof CollectionLikeType)
            && !(type instanceof MapType)
            && !clazz.isInterface()
            && !clazz.isAnnotation()
            && !Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * 返回指定的记录类型的所有字段
     *
     * @param type 需要获取字段的类型
     * @return 该记录类型的所有实例字段
     * @throws IllegalArgumentException 在传入的类型不为记录类型时报错
     */
    @Override
    public GenericField[] recordFields(StaticType type) {
        List<GenericField> fields = new ArrayList<>();
        collectRecordFields(type, fields);
        return fields.toArray(new GenericField[0]);
    }

    protected void collectRecordFields(StaticType type, List<GenericField> collectedFields) {
        if (type instanceof CollectionLikeType) {
            throw new IllegalArgumentException(type + " is not a record type");
        }
        collectParentFields(type.getSuperclass(), collectedFields);
        collectDeclaredFields(type, collectedFields);
    }

    protected void collectParentFields(StaticType superType, List<GenericField> collectedFields) {
        if (superType != null && !GenericTypes.OBJECT_TYPE.equals(superType)) {
            collectRecordFields(superType, collectedFields);
        }
    }

    protected void collectDeclaredFields(StaticType type, List<GenericField> collectedFields) {
        for (GenericField field : type.getDeclaredFields()) {
            if (isRecordField(field)) {
                GenericField resolvedField = field.replaceResolvedType(resolveFieldType(field));
                collectedFields.add(resolvedField);
            }
        }
    }


    /**
     * 判断指定的字段是否为记录的字段
     * @param field 要检测的字段
     * @return 判断是否需作为记录的字段
     */
    protected abstract boolean isRecordField(GenericField field);

    /**
     * 获取传入的泛型字段的声明类型的所有可能运行时类型
     *
     * @param field 要检测的字段
     * @return 返回一个包含了所有运行时可能出现的类型的一个表达
     */
    protected abstract GenericType resolveFieldType(GenericField field);

    protected abstract static class AbstractTypeConcretingReplacer implements GenericTypeReplacer {
        @Override
        public GenericType replaceType(GenericType type) {
            if (type instanceof CollectionLikeType) {
                return type;
            } else if (type instanceof MapType) {
                return type;
            } else if (type instanceof StaticType) {
                return replaceStaticType((StaticType) type);
            } else if (type instanceof WildcardType) {
                return replaceWildcardType((WildcardType) type);
            } else {
                return type;
            }
        }

        protected GenericType replaceStaticType(StaticType type) {
            List<StaticType> concreteTypes = collectConcreteTypes(type);
            if (concreteTypes.isEmpty()) {
                return type;
            } else {
                return GenericTypes.unionType(type, concreteTypes.toArray(new StaticType[0]));
            }
        }

        protected GenericType replaceWildcardType(WildcardType type) {
            GenericType[] upperBounds = type.getUpperBounds();
            GenericType lowerBound = type.getLowerBound();
            if (upperBounds.length == 1 && GenericTypes.OBJECT_TYPE.equals(upperBounds[0])) {
                throw new IllegalArgumentException("unbounded wildcard type " + type + " is not supported");
            }

            GenericType firstBound = upperBounds[0];
            if (!(firstBound instanceof StaticType)) {
                throw new IllegalArgumentException("the first bound " + firstBound + " of the wildcard type "
                    + type + " must be a static type");
            }

            List<StaticType> concreteTypes = collectConcreteTypes((StaticType) firstBound);
            concreteTypes.removeIf(concreteType -> {
                for (int i = 1; i < upperBounds.length; i++) {
                    if (!upperBounds[i].isAssignableFrom(concreteType)) {
                        return true;
                    }
                }
                return lowerBound != null && !concreteType.isAssignableFrom(lowerBound);
            });

            if (concreteTypes.isEmpty()) {
                throw new IllegalArgumentException("no concrete type found for wildcard " + type);
            }

            return GenericTypes.unionType(type, concreteTypes.toArray(new StaticType[0]));
        }

        protected abstract Set<Class<?>> searchSubclasses(StaticType type);

        protected abstract boolean needConcrete(StaticType type);

        private List<StaticType> collectConcreteTypes(StaticType type) {
            Class<?> clazz = type.getRawType();
            if (clazz.isPrimitive()
                || clazz.isEnum()
                || clazz.isAnnotation()
                || clazz.isArray()
                || Modifier.isFinal(clazz.getModifiers())
                || Object.class.equals(clazz)
                || Collection.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz)
                || !needConcrete(type)) {
                return new ArrayList<>();
            }

            Set<Class<?>> subclasses = searchSubclasses(type);
            List<StaticType> concreteTypes = new ArrayList<>(subclasses.size());
            for (Class<?> subclass : subclasses) {
                if (subclass.isInterface()
                    || subclass.isAnonymousClass()
                    || subclass.isSynthetic()
                    || subclass.isLocalClass()
                    || Modifier.isAbstract(subclass.getModifiers())) continue;
                StaticType concreteType = type.asSubType(subclass);
                concreteTypes.add(concreteType);
            }
            return concreteTypes;
        }
    }
}
