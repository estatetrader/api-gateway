package com.estatetrader.generic.impl;

import com.estatetrader.generic.DynamicType;

import java.lang.reflect.AnnotatedElement;

public abstract class AbstractDynamicType extends AbstractGenericType implements DynamicType {
    public AbstractDynamicType(AnnotatedElement element) {
        super(element);
    }
}
