package com.estatetrader.generic;

import com.estatetrader.generic.impl.GenericTypeFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

public final class GenericTypes {

    public static final ClassType OBJECT_TYPE = GenericTypeFactory.OBJECT_TYPE;

    private GenericTypes() {}

    public static GenericType of(AnnotatedType annotatedType) {
        return GenericTypeFactory.toGenericType(annotatedType);
    }

    public static GenericType[] of(AnnotatedType[] annotatedTypes) {
        return GenericTypeFactory.toGenericTypes(annotatedTypes);
    }

    public static StaticType of(Class<?> clazz) {
        return GenericTypeFactory.fromClass(clazz, false);
    }

    public static GenericType of(Type type) {
        return GenericTypeFactory.toGenericType(type);
    }

    public static ArrayType arrayType(ArrayType originType, GenericType componentType) {
        if (originType.getComponentType().equals(componentType)) {
            return originType;
        } else {
            return GenericTypeFactory.arrayType(originType.annotatedElement(), componentType);
        }
    }

    public static ParameterizedType parameterizedType(Class<?> rawType,
                                                      GenericType ownerType,
                                                      GenericType... typeArguments) {
        return GenericTypeFactory.parameterizedType(null, rawType, ownerType, typeArguments);
    }

    public static ParameterizedType parameterizedType(ParameterizedType originType,
                                                      GenericType ownerType,
                                                      GenericType... typeArguments) {
        if (Objects.equals(originType.getOwnerType(), ownerType)
            && Arrays.equals(originType.getTypeArguments(), typeArguments)) {
            return originType;
        } else {
            AnnotatedElement element = originType.annotatedElement();
            Class<?> rawType = originType.getRawType();
            return GenericTypeFactory.parameterizedType(element, rawType, ownerType, typeArguments);
        }
    }

    public static TypeVariable typeVariable(TypeVariable originType, GenericType[] bounds) {
        if (Arrays.equals(originType.getBounds(), bounds)) {
            return originType;
        } else {
            AnnotatedElement element = originType.annotatedElement();
            return GenericTypeFactory.typeVariable(element, originType.getName(), bounds, originType.getGenericDeclaration());
        }
    }

    public static WildcardType wildcardType(GenericType upperBound) {
        return GenericTypeFactory.wildcardType(null, new GenericType[]{upperBound}, null);
    }

    public static WildcardType wildcardType(GenericType[] upperBounds, GenericType lowerBound) {
        return GenericTypeFactory.wildcardType(null, upperBounds, lowerBound);
    }

    public static WildcardType wildcardType(WildcardType originType, GenericType[] upperBounds, GenericType lowerBound) {
        if (Arrays.equals(originType.getUpperBounds(), upperBounds)
            && Objects.equals(originType.getLowerBound(), lowerBound)) {
            return originType;
        } else {
            return GenericTypeFactory.wildcardType(originType.annotatedElement(), upperBounds, lowerBound);
        }
    }

    public static UnionType unionType(GenericType declaredType, StaticType[] possibleTypes) {
        return GenericTypeFactory.unionType(declaredType, possibleTypes);
    }

    public static GenericType parameterType(Parameter parameter) {
        AnnotatedType annotatedType = null;
        try {
            annotatedType = parameter.getAnnotatedType();
        } catch (NullPointerException e) {
            // ignore the bugs in jdk when retrieving annotation info if a ClassNotFound exception occurred
        }
        if (annotatedType != null) {
            return GenericTypeFactory.toGenericType(annotatedType);
        } else {
            return GenericTypeFactory.toGenericType(parameter.getParameterizedType());
        }
    }

    public static GenericType methodReturnType(Method method) {
        AnnotatedType annotatedType = null;
        try {
            annotatedType = method.getAnnotatedReturnType();
        } catch (NullPointerException e) {
            // ignore the bugs in jdk when retrieving annotation info if a ClassNotFound exception occurred
        }
        if (annotatedType != null) {
            return GenericTypeFactory.toGenericType(annotatedType);
        } else {
            return GenericTypeFactory.toGenericType(method.getGenericReturnType());
        }
    }

    public static GenericType fieldType(Field field) {
        AnnotatedType annotatedType = null;
        try {
            annotatedType = field.getAnnotatedType();
        } catch (NullPointerException e) {
            // ignore the bugs in jdk when retrieving annotation info if a ClassNotFound exception occurred
        }
        if (annotatedType != null) {
            return GenericTypeFactory.toGenericType(annotatedType);
        } else {
            return GenericTypeFactory.toGenericType(field.getGenericType());
        }
    }
}
