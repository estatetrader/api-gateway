package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.Objects;

class ArrayTypeImpl extends StaticTypeImpl implements ArrayType {

    private final Class<?> rawType;
    private final GenericType componentType;

    ArrayTypeImpl(AnnotatedElement element, GenericType componentType) {
        super(element);
        this.componentType = componentType;
        if (componentType instanceof StaticType) {
            this.rawType = Array.newInstance(((StaticType) componentType).getRawType(), 0).getClass();
        } else {
            this.rawType = Object[].class;
        }
    }

    /**
     * Returns a {@code Type} object representing the component type
     * of this array. This method creates the component type of the
     * array.  See the declaration of {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * semantics of the creation process for parameterized types and
     * see {@link java.lang.reflect.TypeVariable TypeVariable} for the
     * creation process for type variables.
     *
     * @return a {@code Type} object representing the component type
     * of this array
     * @throws TypeNotPresentException             if the underlying array type's
     *                                             component type refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if  the
     *                                             underlying array type's component type refers to a
     *                                             parameterized type that cannot be instantiated for any reason
     */
    @Override
    public GenericType getComponentType() {
        return componentType;
    }

    /**
     * the element type of the collection or array
     *
     * @return element type
     */
    @Override
    public GenericType getElementType() {
        return componentType;
    }

    /**
     * Returns a string describing this type, including information
     * about any type parameters.
     *
     * @return a string describing this type
     * @since 1.8
     */
    @Override
    public String getTypeName() {
        return componentType.getTypeName() + "[]";
    }

    /**
     * Returns the {@code Type} object representing the class or interface
     * that declared this type.
     *
     * @return the {@code Type} object representing the class or interface
     * that declared this type
     * @since 1.5
     */
    @Override
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     *
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    @Override
    public ArrayType asResolvedType(TypeVariableResolver variableResolver) {
        return GenericTypes.arrayType(this, componentType.asResolvedType(variableResolver));
    }

    /**
     * replaces all the variables in this generic type consulting the specified declaringType.
     * the declaringType is one of the usage of a class type, in which this generic type is used to declare
     * a field type, method return type and parameter types, super class type and interface types.
     *
     * @param declaringType the declaring type in which this generic type is used
     * @return resolved type
     */
    @Override
    public ArrayType asResolvedType(ParameterizedType declaringType) {
        return (ArrayType) super.asResolvedType(declaringType);
    }

    /**
     * combines this type and the given actual-type,
     * if these two types do not match, than an IllegalArgument exception will be thrown.
     * calculates the variables included in this type and its nested types while combine.
     *
     * @param actualType the actual-type used to resolve variables
     * @return the binding of all the variables included in this type and their resolved types,
     * or null if these two types do not match
     */
    @Override
    public VariableBinding combineType(GenericType actualType) {
        if (actualType instanceof ArrayType) {
            return componentType.combineType(((ArrayType) actualType).getComponentType());
        } else {
            return null;
        }
    }

    /**
     * calculate the intersected type of this type and the given type
     *
     * @param another another type
     * @return the intersected type, or null if there is no intersect type between the two types
     */
    @Override
    public GenericType intersect(GenericType another) {
        if (GenericTypeFactory.OBJECT_TYPE.equals(another)) {
            return this;
        } else if (another instanceof ArrayType) {
            ArrayType anotherType = (ArrayType) another;
            GenericType ic = componentType.intersect(anotherType.getComponentType());
            if (ic == null) {
                return null;
            }
            if (componentType.equals(ic)) {
                return new ArrayTypeImpl(element, ic);
            } else if (anotherType.getComponentType().equals(ic)) {
                return new ArrayTypeImpl(anotherType.annotatedElement(), ic);
            } else {
                return new ArrayTypeImpl(null, ic);
            }
        } else if (another instanceof StaticType) {
            return null;
        } else {
            return another.intersect(this);
        }
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link Type}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public Type toReflectType() {
        GenericType componentType = getComponentType();
        if (componentType instanceof ClassType) {
            Class<?> clazz = ((ClassType) componentType).getRawType();
            if (clazz == boolean.class) {
                return boolean[].class;
            } else if (clazz == char.class) {
                return char[].class;
            } else if (clazz == byte.class) {
                return byte[].class;
            } else if (clazz == short.class) {
                return short[].class;
            } else if (clazz == int.class) {
                return int[].class;
            } else if (clazz == long.class) {
                return long[].class;
            } else if (clazz == float.class) {
                return float[].class;
            } else if (clazz == double.class) {
                return double[].class;
            }
        }
        return new ReflectArrayTypeImpl(this);
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link AnnotatedType}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public AnnotatedType toAnnotatedType() {
        return new ReflectArrayTypeImpl(this);
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(GenericType type) {
        if (!(type instanceof ArrayType)) return false;
        ArrayType arrayType = (ArrayType) type;
        return componentType.equals(arrayType.getComponentType());
    }

    /**
     * the origin type when it is declared, the parameterized-type with its type variables, for instance
     *
     * @return the origin declared type of this type
     */
    @Override
    public StaticType getDeclaredType() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentType);
    }

    @Override
    protected GenericType replaceComponents(GenericTypeReplacer replacer) {
        return GenericTypes.arrayType(this, componentType.replace(replacer));
    }

    /**
     * visit this type and all this component types using the given visitor
     *
     * @param visitor the visitor instance used to visit this type
     * @return the final visit report
     */
    @Override
    public <R> R visit(GenericTypeVisitor<R> visitor) {
        return visitor.visitType(this, Collections.singletonList(componentType.visit(visitor)));
    }

    private static class ReflectArrayTypeImpl extends AnnotatedReflectType implements GenericArrayType, AnnotatedArrayType {
        public ReflectArrayTypeImpl(ArrayType genericType) {
            super(genericType);
        }

        // fix jdk11 compatability
        public AnnotatedType getAnnotatedOwnerType() {
            return null;
        }

        /**
         * Returns a {@code Type} object representing the component type
         * of this array. This method creates the component type of the
         * array.  See the declaration of {@link
         * java.lang.reflect.ParameterizedType ParameterizedType} for the
         * semantics of the creation process for parameterized types and
         * see {@link TypeVariable TypeVariable} for the
         * creation process for type variables.
         *
         * @return a {@code Type} object representing the component type
         * of this array
         * @throws TypeNotPresentException             if the underlying array type's
         *                                             component type refers to a non-existent type declaration
         * @throws MalformedParameterizedTypeException if  the
         *                                             underlying array type's component type refers to a
         *                                             parameterized type that cannot be instantiated for any reason
         */
        @Override
        public Type getGenericComponentType() {
            return ((ArrayType) getGenericType()).getComponentType().toReflectType();
        }

        /**
         * Returns the potentially annotated generic component type of this array type.
         *
         * @return the potentially annotated generic component type of this array type
         */
        @Override
        public AnnotatedType getAnnotatedGenericComponentType() {
            return ((ArrayType) getGenericType()).getComponentType().toAnnotatedType();
        }
    }
}
