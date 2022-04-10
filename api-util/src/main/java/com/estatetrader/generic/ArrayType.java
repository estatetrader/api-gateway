package com.estatetrader.generic;

import java.lang.reflect.MalformedParameterizedTypeException;

/**
 * {@code ArrayType} represents an array type whose component
 * type is either a parameterized type or a type variable.
 * @since 1.5
 */
public interface ArrayType extends StaticType, CollectionLikeType {
    /**
     * Returns a {@code Type} object representing the component type
     * of this array. This method creates the component type of the
     * array.  See the declaration of {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * semantics of the creation process for parameterized types and
     * see {@link java.lang.reflect.TypeVariable TypeVariable} for the
     * creation process for type variables.
     *
     * @return  a {@code Type} object representing the component type
     *     of this array
     * @throws TypeNotPresentException if the underlying array type's
     *     component type refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if  the
     *     underlying array type's component type refers to a
     *     parameterized type that cannot be instantiated for any reason
     */
    GenericType getComponentType();

    /**
     * replaces all the variables in this generic type consulting the specified declaringType.
     * the declaringType is one of the usage of a class type, in which this generic type is used to declare
     * a field type, method return type and parameter types, super class type and interface types.
     * @param declaringType the declaring type in which this generic type is used
     * @return resolved type
     */
    ArrayType asResolvedType(ParameterizedType declaringType);

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    ArrayType asResolvedType(TypeVariableResolver variableResolver);
}
