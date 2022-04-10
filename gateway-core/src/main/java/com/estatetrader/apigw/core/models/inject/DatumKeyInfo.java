package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.CollectionLikeType;
import com.estatetrader.generic.GenericType;

public class DatumKeyInfo {
    private final String name;
    private final GenericType type;
    private final DatumKeyReader accessor;

    public DatumKeyInfo(String name, GenericType type, DatumKeyReader accessor) {
        this.name = name;
        this.type = type;
        this.accessor = accessor;
    }

    public Object readKey(Object datum) {
        return accessor.read(datum);
    }

    public String getName() {
        return name;
    }

    public GenericType getType() {
        return type;
    }

    public DatumKeyTypeMatched tryMatchType(GenericType anotherType) {
        DatumKeyEqualityChecker equalityChecker = DatumKeyEqualityChecker.forType(type, anotherType);
        // 类型兼容
        if (equalityChecker != null) {
            return new DatumKeyTypeMatched(false, new SingularDatumKeyMatcher(equalityChecker));
        }

        if (!(anotherType instanceof CollectionLikeType)) {
           return null;
        }

        CollectionLikeType colType = (CollectionLikeType) anotherType;

        GenericType elementType = colType.getElementType();
        DatumKeyEqualityChecker elementEqualityChecker = DatumKeyEqualityChecker.forType(type, elementType);
        if (elementEqualityChecker == null) {
            return null;
        }

        ElementReader elementReader = ElementReader.forType(colType);
        return new DatumKeyTypeMatched(true, new PluralDatumKeyMatcher(elementReader, elementEqualityChecker));
    }

    private class SingularDatumKeyMatcher implements DatumKeyMatcher {
        private final DatumKeyEqualityChecker equalityChecker;

        public SingularDatumKeyMatcher(DatumKeyEqualityChecker equalityChecker) {
            this.equalityChecker = equalityChecker;
        }

        @Override
        public boolean matches(Object datum, Object targetKey) {
            Object datumKey = readKey(datum);
            return equalityChecker.keyEquals(datumKey, targetKey);
        }
    }

    private class PluralDatumKeyMatcher implements DatumKeyMatcher {
        private final ElementReader elementReader;
        private final DatumKeyEqualityChecker equalityChecker;

        public PluralDatumKeyMatcher(ElementReader elementReader, DatumKeyEqualityChecker equalityChecker) {
            this.elementReader = elementReader;
            this.equalityChecker = equalityChecker;
        }

        @Override
        public boolean matches(Object datum, Object targetKey) {
            Object datumKey = readKey(datum);
            if (datumKey == null) {
                return false;
            }
            return elementReader.stream(targetKey).anyMatch(x -> equalityChecker.keyEquals(datumKey, x));
        }
    }
}
