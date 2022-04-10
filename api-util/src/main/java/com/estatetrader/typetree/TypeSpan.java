package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;

public interface TypeSpan {
    GenericType getEndType();
    TypeSpanMetadata getMetadata();
}
