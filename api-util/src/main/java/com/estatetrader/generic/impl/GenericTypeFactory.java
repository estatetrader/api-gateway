package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class GenericTypeFactory {

    public static final ClassType OBJECT_TYPE = new ClassTypeImpl(Object.class);

    public static GenericType toGenericType(AnnotatedType annotatedType) {
        Type type = annotatedType.getType();
        if (type instanceof Class) {
            return fromClass((Class<?>) type, false);
        } else if (type instanceof GenericArrayType) {
            GenericType componentType;
            if (annotatedType instanceof AnnotatedArrayType) {
                componentType = toGenericType(((AnnotatedArrayType) annotatedType).getAnnotatedGenericComponentType());
            } else {
                componentType = toGenericType(((GenericArrayType) type).getGenericComponentType());
            }
            return arrayType(annotatedType, componentType);
        } else if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
            Class<?> rawType = (Class<?>)pt.getRawType();
            GenericType ownerType = pt.getOwnerType() != null ? toGenericType(pt.getOwnerType()) : null;
            GenericType[] arguments;
            if (annotatedType instanceof AnnotatedParameterizedType) {
                arguments = toGenericTypes(((AnnotatedParameterizedType) annotatedType).getAnnotatedActualTypeArguments());
            } else {
                arguments = toGenericTypes(pt.getActualTypeArguments());
            }
            return parameterizedType(annotatedType, rawType, ownerType, arguments);
        } else if (type instanceof java.lang.reflect.TypeVariable) {
            return toTypeVariable((java.lang.reflect.TypeVariable<?>) type);
        } else if (type instanceof java.lang.reflect.WildcardType) {
            GenericType[] upperBounds;
            GenericType[] lowerBounds;
            if (annotatedType instanceof AnnotatedWildcardType) {
                AnnotatedWildcardType awt = (AnnotatedWildcardType) annotatedType;
                upperBounds = toGenericTypes(awt.getAnnotatedUpperBounds());
                lowerBounds = toGenericTypes(awt.getAnnotatedLowerBounds());
            } else {
                java.lang.reflect.WildcardType wt = (java.lang.reflect.WildcardType) type;
                upperBounds = toGenericTypes(wt.getUpperBounds());
                lowerBounds = toGenericTypes(wt.getLowerBounds());
            }
            return wildcardType(annotatedType, upperBounds, lowerBounds.length == 0 ? null : lowerBounds[0]);
        } else {
            throw new IllegalArgumentException("unsupported type " + type);
        }
    }

    public static GenericType[] toGenericTypes(AnnotatedType[] annotatedTypes) {
        GenericType[] genericTypes = new GenericType[annotatedTypes.length];
        for (int i = 0; i < annotatedTypes.length; i++) {
            genericTypes[i] = toGenericType(annotatedTypes[i]);
        }
        return genericTypes;
    }

    public static StaticType fromClass(Class<?> clazz, boolean keepVars) {
        if (clazz.equals(Object.class)) {
            return GenericTypeFactory.OBJECT_TYPE;
        }
        if (clazz.isArray()) {
            return arrayType(clazz, toGenericType(clazz.getComponentType()));
        }
        java.lang.reflect.TypeVariable<?>[] vars = clazz.getTypeParameters();
        if (vars.length > 0) {
            Class<?> declaringClass = clazz.getDeclaringClass();
            GenericType ownerType = declaringClass != null ? fromClass(declaringClass, keepVars) : null;
            GenericType[] args = new GenericType[vars.length];
            for (int i = 0; i < vars.length; i++) {
                if (keepVars) {
                    args[i] = toTypeVariable(vars[i]);
                } else {
                    GenericType[] upperBounds = toGenericTypes(vars[i].getAnnotatedBounds());
                    args[i] = wildcardType(vars[i], upperBounds, null);
                }
            }
            return parameterizedType(clazz, clazz, ownerType, args);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            return new ClassTypeImpl.OfCollection(clazz);
        } else if (Map.class.isAssignableFrom(clazz)) {
            return new ClassTypeImpl.OfMap(clazz);
        } else {
            return new ClassTypeImpl(clazz);
        }
    }

    public static ArrayType arrayType(AnnotatedElement element, GenericType componentType) {
        return new ArrayTypeImpl(element, componentType);
    }

    public static ParameterizedType parameterizedType(AnnotatedElement element,
                                                      Class<?> rawType,
                                                      GenericType ownerType,
                                                      GenericType... typeArguments) {
        if (Collection.class.isAssignableFrom(rawType)) {
            return new ParameterizedTypeImpl.OfCollection(element, rawType, ownerType, typeArguments);
        } else if (Map.class.isAssignableFrom(rawType)) {
            return new ParameterizedTypeImpl.OfMap(element, rawType, ownerType, typeArguments);
        } else {
            return new ParameterizedTypeImpl(element, rawType, ownerType, typeArguments);
        }
    }

    public static TypeVariable typeVariable(AnnotatedElement element, String name, GenericType[] bounds, GenericDeclaration declaration) {
        return new TypeVariableImpl(element, name, bounds, declaration);
    }

    public static WildcardType wildcardType(AnnotatedElement element, GenericType[] upperBounds, GenericType lowerBound) {
        return new WildcardTypeImpl(element, upperBounds, lowerBound);
    }

    public static UnionType unionType(GenericType declaredType, StaticType[] possibleTypes) {
        return new UnionTypeImpl(declaredType, possibleTypes);
    }

    public static GenericType toGenericType(Type type) {
        if (type instanceof Class) {
            return fromClass((Class<?>) type, false);
        } else if (type instanceof GenericArrayType) {
            GenericType componentType = toGenericType(((GenericArrayType) type).getGenericComponentType());
            return arrayType(null, componentType);
        } else if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
            Class<?> rawType = (Class<?>)pt.getRawType();
            GenericType ownerType = pt.getOwnerType() != null ? toGenericType(pt.getOwnerType()) : null;
            GenericType[] arguments = toGenericTypes(pt.getActualTypeArguments());
            return parameterizedType(null, rawType, ownerType, arguments);
        } else if (type instanceof java.lang.reflect.TypeVariable) {
            return toTypeVariable((java.lang.reflect.TypeVariable<?>) type);
        } else if (type instanceof java.lang.reflect.WildcardType) {
            java.lang.reflect.WildcardType wt = (java.lang.reflect.WildcardType) type;
            GenericType[] upperBounds = toGenericTypes(wt.getUpperBounds());
            GenericType[] lowerBounds = toGenericTypes(wt.getLowerBounds());
            return wildcardType(null, upperBounds, lowerBounds.length == 0 ? null : lowerBounds[0]);
        } else if (type == null) {
            throw new NullPointerException("type cannot be null");
        } else {
            throw new IllegalArgumentException("unsupported type " + type);
        }
    }

    static GenericType[] toGenericTypes(Type[] types) {
        GenericType[] genericTypes = new GenericType[types.length];
        for (int i = 0; i < types.length; i++) {
            genericTypes[i] = toGenericType(types[i]);
        }
        return genericTypes;
    }

    static TypeVariable toTypeVariable(java.lang.reflect.TypeVariable<?> var) {
        GenericDeclaration genericDeclaration = var.getGenericDeclaration();
        GenericType[] bounds = toGenericTypes(var.getAnnotatedBounds());
        return typeVariable(var, var.getName(), bounds, genericDeclaration);
    }

    static TypeVariable[] toTypeVariables(java.lang.reflect.TypeVariable<?>[] vars) {
        TypeVariable[] variables = new TypeVariable[vars.length];
        for (int i = 0; i < vars.length; i++) {
            variables[i] = toTypeVariable(vars[i]);
        }
        return variables;
    }
}
