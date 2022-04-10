package com.estatetrader.apigw.core.models;

import com.estatetrader.define.IllegalApiDefinitionException;

import java.util.IdentityHashMap;

public class DynamicTypeObjectMap {
    private final IdentityHashMap<Object, String> typeNameMap = new IdentityHashMap<>();

    public void record(Object object, String typeName) {
        String previous = typeNameMap.put(object, typeName);
        if (previous != null && !previous.equals(typeName)) {
            throw new IllegalApiDefinitionException("conflicted type name in the same object tree "
                + typeName + " and " + previous);
        }
    }

    public String typeNameOf(Object object) {
        return typeNameMap.get(object);
    }
}
