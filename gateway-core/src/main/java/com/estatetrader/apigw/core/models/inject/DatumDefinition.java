package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.StaticType;

public class DatumDefinition {
    private final String datumType;
    private final boolean plural;
    private final StaticType implType;
    private final DatumUnionKeyInfo unionKey;
    private final DatumReader datumReader;

    public DatumDefinition(String datumType, boolean plural, StaticType implType, DatumUnionKeyInfo unionKey, DatumReader datumReader) {
        this.datumType = datumType;
        this.plural = plural;
        this.implType = implType;
        this.unionKey = unionKey;
        this.datumReader = datumReader;
    }

    public String getDatumType() {
        return datumType;
    }

    public boolean isPlural() {
        return plural;
    }

    public StaticType getImplType() {
        return implType;
    }

    public DatumKeyInfo keyInfoOf(String name) {
        return unionKey.keyInfoOf(name);
    }

    public DatumReader getDatumReader() {
        return datumReader;
    }
}
