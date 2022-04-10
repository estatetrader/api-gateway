package com.estatetrader.apigw.core.models.inject;

public class DatumKeyTypeMatched {
    public final boolean plural;
    public final DatumKeyMatcher datumKeyMatcher;

    public DatumKeyTypeMatched(boolean plural, DatumKeyMatcher datumKeyMatcher) {
        this.plural = plural;
        this.datumKeyMatcher = datumKeyMatcher;
    }
}
