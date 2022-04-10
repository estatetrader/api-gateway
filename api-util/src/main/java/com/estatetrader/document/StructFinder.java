package com.estatetrader.document;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StructFinder {
    private final Map<String, TypeStruct> structMap;

    public StructFinder(List<TypeStruct> structures) {
        Map<String, TypeStruct> structMap = new HashMap<>(structures.size());
        for (TypeStruct struct : structures) {
            structMap.put(struct.name, struct);
        }
        this.structMap = structMap;
    }

    public TypeStruct findByName(String name) {
        return structMap.get(name);
    }

    public Collection<TypeStruct> structClosureToType(GenericTypeInfo type) {
        Map<String, TypeStruct> result = new LinkedHashMap<>();
        recursiveFindAllRelatedToType(type, result);
        return result.values();
    }

    public Collection<TypeStruct> structClosureToMethod(MethodInfo mInfo) {
        Map<String, TypeStruct> result = new LinkedHashMap<>();
        if (mInfo.parameters != null) {
            for (ParameterInfo pInfo : mInfo.parameters) {
                recursiveFindAllRelatedToType(pInfo.type, result);
            }
        }
        recursiveFindAllRelatedToType(mInfo.returnType, result);
        return result.values();
    }

    private void recursiveFindAllRelatedToType(GenericTypeInfo type, Map<String, TypeStruct> result) {
        if (type.typeName != null && !result.containsKey(type.typeName)) {
            TypeStruct s = structMap.get(type.typeName);
            if (s != null) {
                result.put(type.typeName, s);
                for (FieldInfo field : s.fields) {
                    recursiveFindAllRelatedToType(field.type, result);
                }
            }
        }

        if (type.typeArgs != null) {
            for (GenericTypeInfo typeArg : type.typeArgs) {
                recursiveFindAllRelatedToType(typeArg, result);
            }
        }

        if (type.possibleTypes != null) {
            for (GenericTypeInfo possibleType : type.possibleTypes.values()) {
                recursiveFindAllRelatedToType(possibleType, result);
            }
        }
    }
}
