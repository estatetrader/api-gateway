package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ParameterizedTypeImpl extends StaticTypeImpl implements ParameterizedType {
    private final Class<?> rawType;
    private final GenericType ownerType;
    private final GenericType[] typeArguments;
    private final TypeVariable[] typeVariables;

    ParameterizedTypeImpl(AnnotatedElement element, Class<?> rawType, GenericType ownerType, GenericType[] typeArguments) {
        super(element);
        this.typeArguments = typeArguments.clone();
        this.rawType = rawType;
        this.ownerType = ownerType;
        this.typeVariables = GenericTypeFactory.toTypeVariables(rawType.getTypeParameters());
        if (ownerType == null && typeArguments.length == 0) {
            throw new IllegalArgumentException("the specified types " + rawType + " is not an valid parameterized type");
        }
        if (this.typeArguments.length != this.typeVariables.length) {
            throw new IllegalArgumentException("the specified type arguments "
                + Arrays.toString(this.typeArguments) + " is not matched with the type variables "
                + Arrays.toString(this.typeVariables) + " defined by " + rawType);
        }
    }

    private ParameterizedTypeImpl(AnnotatedElement element,
                                  Class<?> rawType,
                                  GenericType ownerType,
                                  GenericType[] typeArguments,
                                  TypeVariable[] typeVariables) {
        super(element);
        this.rawType = rawType;
        this.ownerType = ownerType;
        this.typeArguments = typeArguments;
        this.typeVariables = typeVariables;
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
        int length = typeArguments.length;
        if (length == 0) {
            return rawType.getTypeName();
        }

        StringBuilder stringBuilder = new StringBuilder(30 * (length + 1));
        stringBuilder.append(rawType.getTypeName()).append("<").append(typeArguments[0].getTypeName());
        for (int i = 1; i < length; i++) {
            stringBuilder.append(", ").append(typeArguments[i].getTypeName());
        }
        return stringBuilder.append(">").toString();
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
     * Returns a {@code Type} object representing the type that this type
     * is a member of.  For example, if this type is {@code O<T>.I<S>},
     * return a representation of {@code O<T>}.
     *
     * <p>If this type is a top-level type, {@code null} is returned.
     *
     * @return a {@code Type} object representing the type that
     * this type is a member of. If this type is a top-level type,
     * {@code null} is returned
     * @throws TypeNotPresentException             if the owner type
     *                                             refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if the owner type
     *                                             refers to a parameterized type that cannot be instantiated
     *                                             for any reason
     * @since 1.5
     */
    @Override
    public GenericType getOwnerType() {
        return ownerType;
    }

    /**
     * Returns an array of {@code Type} objects representing the actual type
     * arguments to this type.
     *
     * <p>Note that in some cases, the returned array be empty. This can occur
     * if this type represents a non-parameterized type nested within
     * a parameterized type.
     *
     * @return an array of {@code Type} objects representing the actual type
     * arguments to this type
     * @throws TypeNotPresentException             if any of the
     *                                             actual type arguments refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             actual type parameters refer to a parameterized type that cannot
     *                                             be instantiated for any reason
     * @since 1.5
     */
    @Override
    public GenericType[] getTypeArguments() {
        return typeArguments;
    }

    /**
     * Returns an array of {@code TypeVariable} objects that
     * represent the type variables declared by the generic
     * declaration represented by this {@code GenericDeclaration}
     * object, in declaration order.  Returns an array of length 0 if
     * the underlying generic declaration declares no type variables.
     *
     * @return an array of {@code TypeVariable} objects that represent
     * the type variables declared by this generic declaration
     * @throws GenericSignatureFormatError if the generic
     *                                     signature of this generic declaration does not conform to
     *                                     the format specified in
     *                                     <cite>The Java&trade; Virtual Machine Specification</cite>
     */
    @Override
    public TypeVariable[] getTypeParameters() {
        return typeVariables;
    }

    /**
     * find the corresponding type argument for the given type variable
     *
     * @param var the given type variable
     * @return the corresponding type argument, or null if not found
     */
    @Override
    public GenericType findTypeArgument(TypeVariable var) {
        for (int i = 0; i < typeVariables.length; i++) {
            if (typeVariables[i].equals(var)) {
                return typeArguments[i];
            }
        }
        if (ownerType instanceof ParameterizedType) {
            return ((ParameterizedType) ownerType).findTypeArgument(var);
        } else {
            return null;
        }
    }

    /**
     * the origin type when it is declared, the parameterized-type with its type variables, for instance
     *
     * @return the origin declared type of this type
     */
    @Override
    public StaticType getDeclaredType() {
        return GenericTypes.parameterizedType(this, ownerType, typeVariables);
    }

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     *
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    @Override
    public ParameterizedType asResolvedType(TypeVariableResolver variableResolver) {
        GenericType[] resolvedArgs = operateTypes(typeArguments, t -> t.asResolvedType(variableResolver));
        return GenericTypes.parameterizedType(this, ownerType, resolvedArgs);
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
    public ParameterizedType asResolvedType(ParameterizedType declaringType) {
        return (ParameterizedType) super.asResolvedType(declaringType);
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
        if (!(actualType instanceof StaticType)) {
            return null;
        }
        // let these two types in the same level
        StaticType formatted = ((StaticType) actualType).asSuperType(getRawType());
        if (!(formatted instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType another = (ParameterizedType) formatted;
        VariableBinding binding = VariableBinding.EMPTY;
        GenericType anotherOwner = another.getOwnerType();
        if ((ownerType == null) != (anotherOwner == null)) {
            throw new IllegalStateException("the owner type " + ownerType + " in this type " + this
                + " does not match with the owner type " + anotherOwner + " in the actual type " + actualType);
        }
        if (ownerType != null) {
            VariableBinding b = ownerType.combineType(anotherOwner);
            if (b == null) return null;
            binding = binding.merge(b);
        }
        GenericType[] anotherArgs = another.getTypeArguments();
        if (typeArguments.length != anotherArgs.length) {
            throw new IllegalStateException("the type arguments count of this type " + this
                + " is not matched with the count in the actual type " + actualType);
        }
        for (int i = 0; i < typeArguments.length; i++) {
            VariableBinding b = typeArguments[i].combineType(anotherArgs[i]);
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
        if (!(another instanceof ParameterizedType)) {
            return another.intersect(this);
        }
        ParameterizedType anotherType = (ParameterizedType) another;
        ParameterizedType anotherSubType = (ParameterizedType) anotherType.asSubType(this.getRawType());
        if (anotherSubType != null) {
            return intersectWithSameRawType(anotherSubType);
        }
        ParameterizedType thisSubType = (ParameterizedType) this.asSubType(anotherType.getRawType());
        if (thisSubType != null) {
            return anotherType.intersectWithSameRawType(thisSubType);
        }
        return null;
    }

    /**
     * calculate the intersected type of this type and the given type, which has the same raw type with this type
     *
     * @param another another type
     * @return the intersected type, or null if there is no intersect type between the two types
     */
    @Override
    public ParameterizedType intersectWithSameRawType(ParameterizedType another) {
        if (!rawType.equals(another.getRawType())
            || typeArguments.length != another.getTypeArguments().length
            || (ownerType == null) != (another.getOwnerType() == null)) {
            throw new IllegalArgumentException("the specified type " + another + " should be alike with " + this);
        }

        GenericType resultOwner;
        if (ownerType != null) {
            resultOwner = ownerType.intersect(another.getOwnerType());
        } else {
            resultOwner = null;
        }
        GenericType[] anotherTypeArgs = another.getTypeArguments();
        GenericType[] resultArgs = new GenericType[typeArguments.length];
        for (int i = 0; i < typeArguments.length; i++) {
            resultArgs[i] = typeArguments[i].intersect(anotherTypeArgs[i]);
        }
        ParameterizedType resultType = new ParameterizedTypeImpl(element, rawType, resultOwner, resultArgs);
        for (int i = 0; i < typeVariables.length; i++) {
            TypeVariable var = typeVariables[i];
            GenericType[] bounds = var.getBounds();
            for (int j = 0; j < bounds.length; j++) {
                GenericType bound = bounds[i];
                GenericType resolvedBound = bound.asResolvedType(resultType);
                GenericType intersectedArg = resultArgs[i].intersect(resolvedBound);
                if (!intersectedArg.equals(resolvedBound)) {
                    // latter variable bounds may depend on the previous variable's actual type
                    // so we need to create a new parameterized type to reflect the variable's change
                    resultArgs[i] = resultArgs[i].intersect(resolvedBound);
                    resultType = new ParameterizedTypeImpl(element, rawType, resultOwner, resultArgs);
                }
            }
        }
        return resultType;
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link Type}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public Type toReflectType() {
        return new ReflectParameterizedTypeImpl(this);
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link AnnotatedType}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public AnnotatedType toAnnotatedType() {
        return new ReflectParameterizedTypeImpl(this);
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(GenericType type) {
        if (!(type instanceof ParameterizedType)) return false;
        ParameterizedType that = (ParameterizedType) type;
        return rawType.equals(that.getRawType()) &&
            Objects.equals(ownerType, that.getOwnerType()) &&
            Arrays.equals(typeArguments, that.getTypeArguments());
    }

    @Override
    protected GenericType replaceComponents(GenericTypeReplacer replacer) {
        GenericType newOwnerType = ownerType != null ? ownerType.replace(replacer) : null;
        GenericType[] newArgs = operateTypes(typeArguments, t -> t.replace(replacer));
        return GenericTypes.parameterizedType(this, newOwnerType, newArgs);
    }

    /**
     * visit this type and all this component types using the given visitor
     *
     * @param visitor the visitor instance used to visit this type
     * @return the final visit report
     */
    @Override
    public <R> R visit(GenericTypeVisitor<R> visitor) {
        List<R> childrenReports = new ArrayList<>(typeArguments.length);
        for (GenericType typeArg : typeArguments) {
            childrenReports.add(typeArg.visit(visitor));
        }
        return visitor.visitType(this, childrenReports);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rawType, ownerType);
        result = 31 * result + Arrays.hashCode(typeArguments);
        return result;
    }

    static class OfCollection extends ParameterizedTypeImpl implements CollectionType {

        private final GenericType elementType;

        OfCollection(AnnotatedElement element, Class<?> rawType, GenericType ownerType, GenericType[] typeArguments) {
            super(element, rawType, ownerType, typeArguments);
            ParameterizedType pType = (ParameterizedType) asSuperType(Collection.class);
            GenericType[] typeArgs = pType.getTypeArguments();
            if (typeArgs.length != 1) {
                throw new IllegalStateException("the type " + pType + " should contains exactly one type argument");
            }
            this.elementType = typeArgs[0];
        }

        /**
         * the element type of the collection
         *
         * @return element type
         */
        @Override
        public GenericType getElementType() {
            return elementType;
        }
    }

    static class OfMap extends ParameterizedTypeImpl implements MapType {

        private final GenericType keyType;
        private final GenericType valueType;

        OfMap(AnnotatedElement element, Class<?> rawType, GenericType ownerType, GenericType[] typeArguments) {
            super(element, rawType, ownerType, typeArguments);
            ParameterizedType pType = (ParameterizedType) asSuperType(Map.class);
            GenericType[] typeArgs = pType.getTypeArguments();
            if (typeArgs.length != 2) {
                throw new IllegalStateException("the type " + pType + " should contains exactly two type arguments");
            }
            this.keyType = typeArgs[0];
            this.valueType = typeArgs[1];
        }

        /**
         * get the key type of the map
         *
         * @return the generic presentation of the key type
         */
        @Override
        public GenericType getKeyType() {
            return keyType;
        }

        /**
         * get the value type of the map
         *
         * @return the generic presentation of the value type
         */
        @Override
        public GenericType getValueType() {
            return valueType;
        }
    }

    private static class ReflectParameterizedTypeImpl extends AnnotatedReflectType
        implements java.lang.reflect.ParameterizedType, AnnotatedParameterizedType {
        public ReflectParameterizedTypeImpl(ParameterizedType genericType) {
            super(genericType);
        }

        // fix jdk11 compatability
        public AnnotatedType getAnnotatedOwnerType() {
            GenericType type = getGenericType();
            return type != null ? new AnnotatedReflectType(type) : null;
        }

        /**
         * Returns the potentially annotated actual type arguments of this parameterized type.
         *
         * @return the potentially annotated actual type arguments of this parameterized type
         */
        @Override
        public AnnotatedType[] getAnnotatedActualTypeArguments() {
            return toAnnotatedTypes(((ParameterizedType) getGenericType()).getTypeArguments());
        }

        /**
         * Returns an array of {@code Type} objects representing the actual type
         * arguments to this type.
         *
         * <p>Note that in some cases, the returned array be empty. This can occur
         * if this type represents a non-parameterized type nested within
         * a parameterized type.
         *
         * @return an array of {@code Type} objects representing the actual type
         * arguments to this type
         * @throws TypeNotPresentException             if any of the
         *                                             actual type arguments refers to a non-existent type declaration
         * @throws MalformedParameterizedTypeException if any of the
         *                                             actual type parameters refer to a parameterized type that cannot
         *                                             be instantiated for any reason
         * @since 1.5
         */
        @Override
        public Type[] getActualTypeArguments() {
            return toReflectTypes(((ParameterizedType) getGenericType()).getTypeArguments());
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
        public Type getRawType() {
            return ((ParameterizedType) getGenericType()).getRawType();
        }

        /**
         * Returns a {@code Type} object representing the type that this type
         * is a member of.  For example, if this type is {@code O<T>.I<S>},
         * return a representation of {@code O<T>}.
         *
         * <p>If this type is a top-level type, {@code null} is returned.
         *
         * @return a {@code Type} object representing the type that
         * this type is a member of. If this type is a top-level type,
         * {@code null} is returned
         * @throws TypeNotPresentException             if the owner type
         *                                             refers to a non-existent type declaration
         * @throws MalformedParameterizedTypeException if the owner type
         *                                             refers to a parameterized type that cannot be instantiated
         *                                             for any reason
         * @since 1.5
         */
        @Override
        public Type getOwnerType() {
            GenericType ownerType = ((ParameterizedType) getGenericType()).getOwnerType();
            return ownerType != null ? ownerType.toReflectType() : null;
        }
    }
}
