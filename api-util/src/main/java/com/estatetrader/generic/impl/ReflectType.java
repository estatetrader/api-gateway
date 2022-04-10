package com.estatetrader.generic.impl;

import com.estatetrader.generic.GenericType;

import java.lang.reflect.Type;

abstract class ReflectType implements Type {
    private final GenericType genericType;

    protected ReflectType(GenericType genericType) {
        this.genericType = genericType;
    }

    public GenericType getGenericType() {
        return genericType;
    }

    /**
     * Returns a string describing this type, including information
     * about any type parameters.
     *
     * @return a string describing this type
     * @implSpec The default implementation calls {@code toString}.
     * @since 1.8
     */
    @Override
    public String getTypeName() {
        return genericType.getTypeName();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ReflectType)) return false;

        ReflectType that = (ReflectType) object;

        return genericType.equals(that.genericType);
    }

    @Override
    public int hashCode() {
        return genericType.hashCode();
    }

    @Override
    public String toString() {
        return genericType.toString();
    }
}
