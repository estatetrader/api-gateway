package com.estatetrader.apigw.core.models.inject;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface DatumExportedKeyAdaptor {
    Map<String, Object> adapt(List<DatumExportedUnionKey> unionKeys);
}
