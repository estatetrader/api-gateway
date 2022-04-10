package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class UnionTypeImpl extends AbstractDynamicType implements UnionType {
    private final GenericType declaredType;
    private final StaticType[] possibleTypes;

    UnionTypeImpl(GenericType declaredType, StaticType[] possibleTypes) {
        super(null);
        this.declaredType = declaredType;
        this.possibleTypes = possibleTypes;
    }

    /**
     * the type with which this dynamic type represents for
     *
     * @return the declared type
     */
    @Override
    public GenericType getDeclaredType() {
        return declaredType;
    }

    /**
     * all the possible types this dynamic type will represent, any two among these types will not share the same raw-type
     *
     * @return all possible types
     */
    @Override
    public StaticType[] getPossibleTypes() {
        return possibleTypes;
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
        return declaredType.getTypeName();
    }

    @Override
    public String toString() {
        return Arrays.stream(possibleTypes)
            .map(Object::toString)
            .collect(Collectors.joining(",", "union[" + declaredType + "](", ")"));
    }

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     *
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    @Override
    public GenericType asResolvedType(TypeVariableResolver variableResolver) {
        StaticType[] newPossibleTypes = operateTypes(possibleTypes, t -> t.asResolvedType(variableResolver));
        if (Arrays.equals(possibleTypes, newPossibleTypes)) {
            return this;
        } else {
            return new UnionTypeImpl(declaredType, newPossibleTypes);
        }
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
    public UnionType asResolvedType(ParameterizedType declaringType) {
        return (UnionType) super.asResolvedType(declaringType);
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
        for (GenericType concreteType : possibleTypes) {
            VariableBinding b = concreteType.combineType(actualType);
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
            StaticType anotherType = (StaticType) another;
            for (StaticType concreteType : possibleTypes) {
                if (concreteType.equals(anotherType)) {
                    return another;
                }
            }
            return null;
        }
        if (another instanceof UnionType) {
            UnionType anotherType = (UnionType) another;
            StaticType[] anotherPossibleTypes = anotherType.getPossibleTypes();
            List<StaticType> newPossibleTypes = new ArrayList<>(possibleTypes.length);
            for (StaticType t1 : possibleTypes) {
                for (StaticType t2 : anotherPossibleTypes) {
                    if (t1.equals(t2)) {
                        newPossibleTypes.add(t1);
                        break;
                    }
                }
            }
            if (newPossibleTypes.isEmpty()) {
                return null;
            }
            return new UnionTypeImpl(declaredType, newPossibleTypes.toArray(new StaticType[0]));
        }
        return null;
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
        return possibleTypes;
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
        return possibleTypes;
    }

    /**
     * check if this type can be assigned by the given type another.
     *
     * @param another the another type
     * @return true if the assigment can be archived
     */
    @Override
    public boolean isAssignableFrom(GenericType another) {
        for (GenericType possibleType : possibleTypes) {
            if (!possibleType.isAssignableFrom(another)) {
                return false;
            }
        }
        return true;
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link Type}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public Type toReflectType() {
        return declaredType.toReflectType();
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link AnnotatedType}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public AnnotatedType toAnnotatedType() {
        return declaredType.toAnnotatedType();
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(GenericType type) {
        if (!(type instanceof UnionType)) return false;
        UnionType that = (UnionType) type;
        return declaredType.equals(that.getDeclaredType()) && Arrays.equals(possibleTypes, that.getPossibleTypes());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(declaredType);
        result = 31 * result + Arrays.hashCode(possibleTypes);
        return result;
    }

    @Override
    protected GenericType replaceComponents(GenericTypeReplacer replacer) {
        List<StaticType> newPossibleTypes = new ArrayList<>(possibleTypes.length);
        for (GenericType possibleType : possibleTypes) {
            GenericType t = replacer.replaceType(possibleType);
            if (t instanceof StaticType) {
                StaticType newPossibleType = (StaticType) t;
                if (!newPossibleTypes.contains(newPossibleType)) {
                    newPossibleTypes.add(newPossibleType);
                }
            } else if (t instanceof UnionType) {
                for (StaticType newPossibleType : ((UnionType) t).getPossibleTypes()) {
                    if (!newPossibleTypes.contains(newPossibleType)) {
                        newPossibleTypes.add(newPossibleType);
                    }
                }
            } else {
                throw new IllegalArgumentException("the replaced new type " + t +
                    " for the dynamic type " + this + " should be GenericRealType or DynamicType");
            }
        }
        return new UnionTypeImpl(declaredType, newPossibleTypes.toArray(new StaticType[0]));
    }

    /**
     * visit this type and all this component types using the given visitor
     *
     * @param visitor the visitor instance used to visit this type
     * @return the final visit report
     */
    @Override
    public <R> R visit(GenericTypeVisitor<R> visitor) {
        List<R> childrenReports = new ArrayList<>(possibleTypes.length);
        for (GenericType possibleType : possibleTypes) {
            childrenReports.add(possibleType.visit(visitor));
        }
        return visitor.visitType(this, childrenReports);
    }
}
