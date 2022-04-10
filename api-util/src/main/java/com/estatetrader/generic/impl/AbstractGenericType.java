package com.estatetrader.generic.impl;

import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.GenericTypeReplacer;
import com.estatetrader.generic.ParameterizedType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.UnaryOperator;

public abstract class AbstractGenericType extends AnnotatedGenericType {
    public AbstractGenericType(AnnotatedElement element) {
        super(element);
    }

    @Override
    public String toString() {
        return getTypeName();
    }

    @Override
    public abstract int hashCode();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof GenericType) {
            return equals((GenericType) obj);
        }
        if (obj instanceof AnnotatedType) {
            return equals(GenericTypeFactory.toGenericType((AnnotatedType) obj));
        }
        if (obj instanceof Type) {
            return equals(GenericTypeFactory.toGenericType((Type) obj));
        }
        return false;
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
    public GenericType asResolvedType(ParameterizedType declaringType) {
        return asResolvedType(var -> {
            GenericType type = declaringType.findTypeArgument(var);
            if (type == null) {
                throw new IllegalArgumentException("could not find type variable " + var
                    + " in the declaring type " + declaringType);
            }
            return type;
        });
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(Type type) {
        return equals(GenericTypeFactory.toGenericType(type));
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(AnnotatedType type) {
        return equals(GenericTypeFactory.toGenericType(type));
    }

    /**
     * replace the component types of this type with the given replacer
     *
     * @param replacer the replacer which specifies the type replacement logic
     * @return the replaced type
     */
    @Override
    public GenericType replace(GenericTypeReplacer replacer) {
        return ((AbstractGenericType) replacer.replaceType(this)).replaceComponents(replacer);
    }

    protected abstract GenericType replaceComponents(GenericTypeReplacer replacer);

    static <T extends GenericType> T[] operateTypes(T[] types, UnaryOperator<T> operator) {
        T[] result = null;
        for (int i = 0; i < types.length; i++) {
            T type = operator.apply(types[i]);
            if (types[i].equals(type)) continue;
            // gc friendly
            if (result == null) {
                result = Arrays.copyOf(types, types.length);
            }
            result[i] = type;
        }
        return result != null ? result : types;
    }
}
