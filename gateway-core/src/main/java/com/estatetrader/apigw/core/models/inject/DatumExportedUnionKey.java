package com.estatetrader.apigw.core.models.inject;

import java.util.Arrays;

public class DatumExportedUnionKey {
    private final DatumExportedKey[] keys;

    public DatumExportedUnionKey(DatumExportedKey[] keys) {
        this.keys = keys;
    }

    public DatumExportedKey keyOfName(String name) {
        for (DatumExportedKey key : keys) {
            if (key.getName().equals(name)) {
                return key;
            }
        }
        return null;
    }

    // for test
    public DatumExportedKey[] getKeys() {
        return keys;
    }

    public int size() {
        return keys.length;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DatumExportedUnionKey)) return false;
        DatumExportedUnionKey unionKey = (DatumExportedUnionKey) object;
        return Arrays.equals(keys, unionKey.keys);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keys);
    }
}
