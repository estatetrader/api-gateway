package com.estatetrader.apigw.core.models.inject;

import java.util.List;

public class DatumUnionKeyInfo {
    private final List<DatumKeyInfo> keyInfos;

    public DatumUnionKeyInfo(List<DatumKeyInfo> keyInfos) {
        this.keyInfos = keyInfos;
    }

    public DatumKeyInfo keyInfoOf(String name) {
        for (DatumKeyInfo keyInfo : keyInfos) {
            if (keyInfo.getName().equals(name)) {
                return keyInfo;
            }
        }
        return null;
    }
}
