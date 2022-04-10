package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;

public interface VisitorContext<T> {
    T visitChildren();
    T visit(GenericType newType);
    RecordTypeResolver typeResolver();
}
