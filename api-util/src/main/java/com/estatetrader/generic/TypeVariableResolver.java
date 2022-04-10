package com.estatetrader.generic;

@FunctionalInterface
public interface TypeVariableResolver {
    GenericType resolve(TypeVariable var);
}
