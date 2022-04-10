package com.estatetrader.generic;

/**
 * the type which represents all the possible concrete types at the compiling time
 */
public interface UnionType extends DynamicType {
    /**
     * the type with which this dynamic type represents for
     * @return the declared type
     */
    GenericType getDeclaredType();

    /**
     * all the possible types that this dynamic type will represent, any two among these types will not share the same raw-type
     * @return all possible types
     */
    StaticType[] getPossibleTypes();

    /**
     * replaces all the variables in this generic type consulting the specified declaringType.
     * the declaringType is one of the usage of a class type, in which this generic type is used to declare
     * a field type, method return type and parameter types, super class type and interface types.
     * @param declaringType the declaring type in which this generic type is used
     * @return resolved type
     */
    UnionType asResolvedType(ParameterizedType declaringType);
}
