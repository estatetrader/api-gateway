package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

class WildcardTypeImpl extends AbstractDynamicType implements WildcardType {
    private final GenericType[] upperBounds;
    private final GenericType lowerBound;

    WildcardTypeImpl(AnnotatedElement element, GenericType[] upperBounds, GenericType lowerBound) {
        super(element);
        this.upperBounds = upperBounds;
        this.lowerBound = lowerBound;
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
        if (lowerBound != null) {
            return "? super " + lowerBound.getTypeName();
        } else {
            StringBuilder sb = new StringBuilder();
            for (GenericType type : upperBounds) {
                if (type instanceof ClassType && ((ClassType) type).getRawType().equals(Object.class)) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(type);
            }
            if (sb.length() == 0) {
                return "?";
            } else {
                return "? extends " + sb.toString();
            }
        }
    }

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
     * type variable
     * @throws TypeNotPresentException             if any of the
     *                                             bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             bounds refer to a parameterized type that cannot be instantiated
     *                                             for any reason
     */
    @Override
    public GenericType[] getUpperBounds() {
        return upperBounds;
    }

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
     * type variable
     * @throws TypeNotPresentException             if any of the
     *                                             bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             bounds refer to a parameterized type that cannot be instantiated
     *                                             for any reason
     */
    @Override
    public GenericType getLowerBound() {
        return lowerBound;
    }

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     *
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    @Override
    public GenericType asResolvedType(TypeVariableResolver variableResolver) {
        GenericType[] newUpperBounds = operateTypes(upperBounds, t -> t.asResolvedType(variableResolver));
        GenericType newLowerBound = lowerBound != null ? lowerBound.asResolvedType(variableResolver) : null;
        return GenericTypes.wildcardType(this, newUpperBounds, newLowerBound);
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
    public WildcardType asResolvedType(ParameterizedType declaringType) {
        return (WildcardType) super.asResolvedType(declaringType);
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
        VariableBinding binding = VariableBinding.EMPTY;
        for (GenericType bound : upperBounds) {
            VariableBinding b = bound.combineType(actualType);
            if (b == null) return null;
            binding = binding.merge(b);
        }
        if (lowerBound != null) {
            VariableBinding b = actualType.combineType(lowerBound);
            if (b == null) return null;
            binding = binding.merge(b);
        }
        return binding;
    }

    /**
     * calculate the intersected type of this type and the given type
     *
     * @param another another type
     * @return the intersected type, or null if there is no intersect type between the two types
     */
    @Override
    public GenericType intersect(GenericType another) {
        if (this.equals(another)) {
            return this;
        }
        if (another instanceof StaticType) {
            for (GenericType bound : upperBounds) {
                if (bound.equals(another)) {
                    return this;
                }
            }
            GenericType[] newUpperBounds = Arrays.copyOf(upperBounds, upperBounds.length + 1);
            newUpperBounds[newUpperBounds.length - 1] = another;
            return new WildcardTypeImpl(element, newUpperBounds, lowerBound);
        }
        return null;
    }

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
     * type variable
     * @throws TypeNotPresentException             if any of the
     *                                             bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             bounds refer to a parameterized type that cannot be instantiated
     *                                             for any reason
     */
    @Override
    public GenericType[] getLowerBounds() {
        return lowerBound != null ? new GenericType[] {lowerBound} : new GenericType[0];
    }
    /**
     * check if this type can be assigned by the given type another.
     *
     * @param another the another type
     * @return true if the assigment can be archived
     */
    @Override
    public boolean isAssignableFrom(GenericType another) {
        if (this.equals(another)) {
            return true;
        }
        for (GenericType upperBound : upperBounds) {
            if (!upperBound.isAssignableFrom(another)) {
                return false;
            }
        }
        return lowerBound == null || another.isAssignableFrom(lowerBound);
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link Type}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public Type toReflectType() {
        return new ReflectWildcardTypeImpl(this);
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link AnnotatedType}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public AnnotatedType toAnnotatedType() {
        return new ReflectWildcardTypeImpl(this);
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(GenericType type) {
        if (!(type instanceof WildcardType)) return false;
        WildcardType that = (WildcardType) type;
        return Arrays.equals(upperBounds, that.getUpperBounds()) &&
            Objects.equals(lowerBound, that.getLowerBound());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(lowerBound);
        result = 31 * result + Arrays.hashCode(upperBounds);
        return result;
    }

    @Override
    protected GenericType replaceComponents(GenericTypeReplacer replacer) {
        return this;
    }

    /**
     * visit this type and all this component types using the given visitor
     *
     * @param visitor the visitor instance used to visit this type
     * @return the final visit report
     */
    @Override
    public <R> R visit(GenericTypeVisitor<R> visitor) {
        return visitor.visitType(this, Collections.emptyList());
    }

    private static class ReflectWildcardTypeImpl extends AnnotatedReflectType
        implements java.lang.reflect.WildcardType, AnnotatedWildcardType {
        public ReflectWildcardTypeImpl(WildcardType genericType) {
            super(genericType);
        }

        // fix jdk11 compatability
        public AnnotatedType getAnnotatedOwnerType() {
            return null;
        }

        /**
         * Returns the potentially annotated lower bounds of this wildcard type.
         *
         * @return the potentially annotated lower bounds of this wildcard type
         */
        @Override
        public AnnotatedType[] getAnnotatedLowerBounds() {
            GenericType lowerBound = ((WildcardType) getGenericType()).getLowerBound();
            if (lowerBound != null) {
                return new AnnotatedType[]{lowerBound.toAnnotatedType()};
            } else {
                return new AnnotatedType[0];
            }
        }

        /**
         * Returns the potentially annotated upper bounds of this wildcard type.
         *
         * @return the potentially annotated upper bounds of this wildcard type
         */
        @Override
        public AnnotatedType[] getAnnotatedUpperBounds() {
            return toAnnotatedTypes(((WildcardType) getGenericType()).getUpperBounds());
        }

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
         * type variable
         * @throws TypeNotPresentException             if any of the
         *                                             bounds refers to a non-existent type declaration
         * @throws MalformedParameterizedTypeException if any of the
         *                                             bounds refer to a parameterized type that cannot be instantiated
         *                                             for any reason
         */
        @Override
        public Type[] getUpperBounds() {
            return toReflectTypes(((WildcardType) getGenericType()).getUpperBounds());
        }

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
         * type variable
         * @throws TypeNotPresentException             if any of the
         *                                             bounds refers to a non-existent type declaration
         * @throws MalformedParameterizedTypeException if any of the
         *                                             bounds refer to a parameterized type that cannot be instantiated
         *                                             for any reason
         */
        @Override
        public Type[] getLowerBounds() {
            GenericType lowerBound = ((WildcardType) getGenericType()).getLowerBound();
            if (lowerBound != null) {
                return new Type[]{lowerBound.toReflectType()};
            } else {
                return new Type[0];
            }
        }
    }
}
