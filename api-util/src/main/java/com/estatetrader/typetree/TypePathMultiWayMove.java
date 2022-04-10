package com.estatetrader.typetree;

import java.util.List;

public interface TypePathMultiWayMove extends TypePathMove {
    List<? extends TypeSpan> next();
}
