package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.CollectionLikeType;
import com.estatetrader.generic.GenericType;

public class DatumImportedKeyInfo {
    private final String name;
    private final GenericType type;
    private final boolean plural;
    private final boolean required;

    public DatumImportedKeyInfo(String name, GenericType type, boolean plural, boolean required) {
        this.name = name;
        this.type = type;
        this.plural = plural;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public GenericType getType() {
        return type;
    }

    public boolean isPlural() {
        return plural;
    }

    public boolean isRequired() {
        return required;
    }

    public DatumKeyTypeMatched tryMatchType(GenericType anotherType) {
        DatumKeyEqualityChecker equalityChecker = DatumKeyEqualityChecker.forType(type, anotherType);
        // 类型兼容
        if (equalityChecker != null) {
            // imported-key和exported-key的关系为一对一或多对多
            return new DatumKeyTypeMatched(plural, null);
        } else if (plural) {
            // imported-key和exported-key的关系为多对一
            GenericType eleType = ((CollectionLikeType) type).getElementType();
            if (DatumKeyEqualityChecker.forType(eleType, anotherType) != null) {
                return new DatumKeyTypeMatched(false, null);
            } else {
                return null;
            }
        } else if (anotherType instanceof CollectionLikeType) {
            // imported-key和exported-key的关系为一对多
            GenericType anotherElementType = ((CollectionLikeType) anotherType).getElementType();
            if (DatumKeyEqualityChecker.forType(type, anotherElementType) != null) {
                return new DatumKeyTypeMatched(true, null);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
