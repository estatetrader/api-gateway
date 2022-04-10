package com.estatetrader.typetree;

import com.estatetrader.generic.StaticType;

import java.util.List;

public interface RecordTypeMove extends TypePathMultiWayMove {
    StaticType recordType();
    List<FieldSpan> fieldSpans();
}
