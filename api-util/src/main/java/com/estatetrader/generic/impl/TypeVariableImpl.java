package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Objects;

class TypeVariableImpl extends AbstractDynamicType implements TypeVariable {
    private final String name;
    private final GenericType[] bounds;
    private final GenericDeclaration genericDeclaration;

    TypeVariableImpl(AnnotatedElement element, String name, GenericType[] bounds, GenericDeclaration genericDeclaration) {
        super(element);
        this.bounds = bounds;
        this.genericDeclaration = genericDeclaration;
        this.name = name;
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
        return name;
    }

    /**
     * Returns an array of {@code Type} objects representing the
     * upper bound(s) of this type variable.  Note that if no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * <p>For each upper bound B: <ul> <li>if B is a parameterized
     * type or a type variable, it is created, (see {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * details of the creation process for parameterized types).
     * <li>Otherwise, B is resolved.  </ul>
     *
     * @return an array of {@code Type}s representing the upper
     * bound(s) of this type variable
     * @throws TypeNotPresentException             if any of the
     *                                             bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             bounds refer to a parameterized type that cannot be instantiated
     *                                             for any reason
     */
    @Override
    public GenericType[] getBounds() {
        return bounds;
    }

    /**
     * Returns the {@code GenericDeclaration} object representing the
     * generic declaration declared this type variable.
     *
     * @return the generic declaration declared for this type variable.
     * @since 1.5
     */
    @Override
    public GenericDeclaration getGenericDeclaration() {
        return genericDeclaration;
    }

    /**
     * Returns the name of this type variable, as it occurs in the source code.
     *
     * @return the name of this type variable, as it appears in the source code
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     *
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    @Override
    public GenericType asResolvedType(TypeVariableResolver variableResolver) {
        return Objects.requireNonNull(variableResolver.resolve(this));
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
        VariableBinding binding = new VariableBinding(this, actualType);
        // if we let the specified actual type assign to this variable, than all the bounds must be met,
        // some bound may referring variables, so we also need to consider the variable binding in these combine
        for (GenericType bound : bounds) {
            VariableBinding b = bound.combineType(actualType);
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
        } else {
            return null;
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
        return bounds;
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
        return new GenericType[0];
    }
    /**
     * check if this type can be assigned by the given type another.
     *
     * @param another the another type
     * @return true if the assigment can be archived
     */
    @Override
    public boolean isAssignableFrom(GenericType another) {
        return this.equals(another);
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link Type}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public Type toReflectType() {
        return new ReflectTypeVariableImpl(this);
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link AnnotatedType}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public AnnotatedType toAnnotatedType() {
        return new ReflectTypeVariableImpl(this);
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(GenericType type) {
        if (!(type instanceof TypeVariable)) return false;
        TypeVariable that = (TypeVariable) type;
        return name.equals(that.getName()) &&
            genericDeclaration.equals(that.getGenericDeclaration());
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

    @Override
    public int hashCode() {
        return Objects.hash(name, genericDeclaration);
    }

    private static class ReflectTypeVariableImpl extends AnnotatedReflectType
        implements java.lang.reflect.TypeVariable<GenericDeclaration>, AnnotatedTypeVariable {
        public ReflectTypeVariableImpl(GenericType genericType) {
            super(genericType);
        }

        // fix jdk11 compatability
        public AnnotatedType getAnnotatedOwnerType() {
            return null;
        }

        /**
         * Returns an array of {@code Type} objects representing the
         * upper bound(s) of this type variable.  Note that if no upper bound is
         * explicitly declared, the upper bound is {@code Object}.
         *
         * <p>For each upper bound B: <ul> <li>if B is a parameterized
         * type or a type variable, it is created, (see {@link
         * ParameterizedType ParameterizedType} for the
         * details of the creation process for parameterized types).
         * <li>Otherwise, B is resolved.  </ul>
         *
         * @return an array of {@code Type}s representing the upper
         * bound(s) of this type variable
         * @throws TypeNotPresentException             if any of the
         *                                             bounds refers to a non-existent type declaration
         * @throws MalformedParameterizedTypeException if any of the
         *                                             bounds refer to a parameterized type that cannot be instantiated
         *                                             for any reason
         */
        @Override
        public Type[] getBounds() {
            return toReflectTypes(((TypeVariable) getGenericType()).getBounds());
        }

        /**
         * Returns the {@code GenericDeclaration} object representing the
         * generic declaration declared this type variable.
         *
         * @return the generic declaration declared for this type variable.
         * @since 1.5
         */
        @Override
        public GenericDeclaration getGenericDeclaration() {
            return ((TypeVariable) getGenericType()).getGenericDeclaration();
        }

        /**
         * Returns the name of this type variable, as it occurs in the source code.
         *
         * @return the name of this type variable, as it appears in the source code
         */
        @Override
        public String getName() {
            return ((TypeVariable) getGenericType()).getName();
        }

        /**
         * Returns an array of AnnotatedType objects that represent the use of
         * types to denote the upper bounds of the type parameter represented by
         * this TypeVariable. The order of the objects in the array corresponds to
         * the order of the bounds in the declaration of the type parameter.
         * <p>
         * Returns an array of length 0 if the type parameter declares no bounds.
         *
         * @return an array of objects representing the upper bounds of the type variable
         * @since 1.8
         */
        @Override
        public AnnotatedType[] getAnnotatedBounds() {
            return toAnnotatedTypes(((TypeVariable) getGenericType()).getBounds());
        }
    }
}
