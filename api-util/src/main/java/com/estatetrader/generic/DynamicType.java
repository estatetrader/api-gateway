package com.estatetrader.generic;

import java.lang.reflect.MalformedParameterizedTypeException;

public interface DynamicType extends GenericType {
    /**
     * Returns an array of {@code Type} objects representing the  upper
     * bound(s) of this type variable.  Note that if no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * <p>For each upper bound B :
     * <ul>
     *  <li>if B is a parameterized type or a type variable, it is created,
     *  (see {@link java.lang.reflect.ParameterizedType ParameterizedType}
     *  for the details of the creation process for parameterized types).
     *  <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the upper bound(s) of this
     *     type variable
     * @throws TypeNotPresentException if any of the
     *     bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *     bounds refer to a parameterized type that cannot be instantiated
     *     for any reason
     */
    GenericType[] getUpperBounds();

    /**
     * Returns an array of {@code Type} objects representing the
     * lower bound(s) of this type variable.  Note that if no lower bound is
     * explicitly declared, the lower bound is the type of {@code null}.
     * In this case, a zero length array is returned.
     *
     * <p>For each lower bound B :
     * <ul>
     *   <li>if B is a parameterized type or a type variable, it is created,
     *  (see {@link java.lang.reflect.ParameterizedType ParameterizedType}
     *  for the details of the creation process for parameterized types).
     *   <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the lower bound(s) of this
     *     type variable
     * @throws TypeNotPresentException if any of the
     *     bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *     bounds refer to a parameterized type that cannot be instantiated
     *     for any reason
     */
    GenericType[] getLowerBounds();
}
