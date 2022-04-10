package com.estatetrader.gateway;

import com.estatetrader.annotation.AllowedTypes;
import com.estatetrader.generic.*;
import com.estatetrader.typetree.AbstractRecordTypeResolver;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.generic.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 被网关管理的出现在返回值、入参或它们的字段以及其嵌套字段位置的实体类型
 */
public class StructTypeResolver extends AbstractRecordTypeResolver {
    /**
     * 获取传入的泛型类型的所有可能运行时类型
     *
     * @param type 要检测的类型
     * @return 返回一个包含了所有运行时可能出现的类型的一个表达
     */
    @Override
    public GenericType concreteType(GenericType type) {
        return type.replace(new EntityTypeConcretingReplacer());
    }

    /**
     * 解析方法的返回值类型
     * @param method 要解析的方法
     * @return 返回解析后的返回值类型
     */
    public GenericType concreteReturnType(Method method) {
        return GenericTypes.methodReturnType(method).replace(new EntityTypeConcretingReplacer(method));
    }

    @Override
    protected boolean isRecordField(GenericField field) {
        return field.isPublic() && !field.isStatic();
    }

    /**
     * 获取传入的泛型字段的声明类型的所有可能运行时类型
     *
     * @param field 要检测的字段
     * @return 返回一个包含了所有运行时可能出现的类型的一个表达
     */
    @Override
    protected GenericType resolveFieldType(GenericField field) {
        return field.getResolvedType().replace(new EntityTypeConcretingReplacer(field));
    }

    @Override
    protected void collectParentFields(StaticType superType, List<GenericField> collectedFields) {
        // 临时忽略父类型的字段，todo 在API定义规范化后取消以下注释
//        super.collectParentFields(superType, collectedFields);
    }

    protected static class WildcardTypeCollector implements GenericTypeVisitor<Set<WildcardType>> {
        @Override
        public Set<WildcardType> visitType(GenericType type, List<Set<WildcardType>> childrenReports) {
            if (type instanceof WildcardType) {
                return Stream.concat(
                    Stream.of((WildcardType) type),
                    childrenReports.stream().flatMap(Set::stream)
                ).collect(Collectors.toSet());
            } else {
                return childrenReports.stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            }
        }
    }

    protected static class EntityTypeConcretingReplacer extends AbstractTypeConcretingReplacer {

        private final Method currentMethod;
        private final GenericField currentField;
        private final GenericType rootType;
        private final Set<WildcardType> wildcardTypes;

        public EntityTypeConcretingReplacer(GenericField currentField) {
            this.currentMethod = null;
            this.currentField = Objects.requireNonNull(currentField);
            this.rootType = currentField.getResolvedType();
            this.wildcardTypes = this.rootType.visit(new WildcardTypeCollector());
        }

        public EntityTypeConcretingReplacer(Method currentMethod) {
            this.currentMethod = Objects.requireNonNull(currentMethod);
            this.currentField = null;
            this.rootType = GenericTypes.methodReturnType(currentMethod);
            this.wildcardTypes = rootType.visit(new WildcardTypeCollector());
        }

        public EntityTypeConcretingReplacer() {
            this.currentMethod = null;
            this.currentField = null;
            this.rootType = null;
            this.wildcardTypes = Collections.emptySet();
        }

        @Override
        protected GenericType replaceWildcardType(WildcardType type) {
            /*
             * JDK BUG
             * 位置：sun.reflect.annotation.AnnotatedTypeFactory.AnnotatedParameterizedTypeImpl.getAnnotatedActualTypeArguments
             * 表现：如果泛型类型本身定义为另一个类的嵌套类型，则在使用该泛型类型时，添加在泛型的某个类型参数值上的注解会被丢弃
             * 例如，如果我们有以下泛型类的定义：
             *
             * class A<T> {}
             *
             * 在某个函数的返回值或实体字段类型上以如下方式使用了该类：
             *
             * A<@AllowedTypes{...} ?>
             *
             * 那么，如果A声明为一个嵌套类型，则通配符上的@AllowedTypes无法被提取；
             * 但是，如果A声明为一个顶层类型，则@AllowedTypes可以被发现
             *
             * 结论，AllowedTypes不支持嵌套类型
             */
            AllowedTypes allowedTypes = type.getAnnotation(AllowedTypes.class);
            if (allowedTypes == null && currentField != null) {
                if (wildcardTypes.contains(type)) {
                    if (wildcardTypes.size() == 1) {
                        allowedTypes = currentField.getAnnotation(AllowedTypes.class);
                    } else {
                        throw new IllegalApiDefinitionException("when the @AllowedTypes is used on field " + currentField
                            + ", exactly one wildcard type is supported");
                    }
                } else {
                    throw new IllegalStateException("invalid wildcard type " + type);
                }
            }
            if (allowedTypes == null && currentMethod != null) {
                if (wildcardTypes.contains(type)) {
                    if (wildcardTypes.size() == 1) {
                        allowedTypes = currentMethod.getAnnotation(AllowedTypes.class);
                    } else {
                        throw new IllegalApiDefinitionException("when the @AllowedTypes is used on method " + currentMethod
                            + ", exactly one wildcard type is supported");
                    }
                } else {
                    throw new IllegalStateException("invalid wildcard type " + type);
                }
            }
            if (allowedTypes == null) {
                throw new IllegalApiDefinitionException("@AllowTypes is required when using wildcard types: " + type);
            }
            Class<?>[] allowedClasses = allowedTypes.value();
            if (allowedClasses.length == 0) {
                throw new IllegalApiDefinitionException("the classes specified in the @AllowedTypes should not be empty");
            }
            Set<Class<?>> set = new LinkedHashSet<>(allowedClasses.length);
            Collections.addAll(set, allowedClasses);
            return createUnionType(type, set);
        }

        private UnionType createUnionType(GenericType declaredType, Set<Class<?>> allowedClasses) {
            StaticType[] allowedTypes = allowedClasses.stream().map(GenericTypes::of).toArray(StaticType[]::new);
            return GenericTypes.unionType(declaredType, allowedTypes);
        }

        @Override
        protected Set<Class<?>> searchSubclasses(StaticType type) {
            AllowedTypes allowedTypes = searchAllowedTypesAnnotation(type);
            if (allowedTypes == null) {
                return Collections.emptySet();
            }
            Class<?>[] classes = allowedTypes.value();
            if (classes.length == 0) {
                throw new IllegalApiDefinitionException("the AllowedTypes on " + type + " cannot be empty");
            }
            return new HashSet<>(Arrays.asList(classes));
        }

        @Override
        protected boolean needConcrete(StaticType type) {
            if (searchAllowedTypesAnnotation(type) != null) {
                if (!Modifier.isAbstract(type.getRawType().getModifiers())) {
                    throw new IllegalApiDefinitionException("the @AllowedTypes can only be used on " +
                        "an abstract or interface type, not on " + type);
                }
                return true;
            }
            return false;
        }

        private AllowedTypes searchAllowedTypesAnnotation(StaticType type) {
            AllowedTypes at = type.getAnnotation(AllowedTypes.class);
            if (at != null) {
                return at;
            }
            at = type.getRawType().getAnnotation(AllowedTypes.class);
            if (at != null) {
                return at;
            }
            if (type.equals(rootType)) {
                if (currentField != null) {
                    at = currentField.getAnnotation(AllowedTypes.class);
                } else if (currentMethod != null) {
                    at = currentMethod.getAnnotation(AllowedTypes.class);
                } else {
                    throw new IllegalStateException("bug");
                }
            }
            return at;
        }
    }
}
