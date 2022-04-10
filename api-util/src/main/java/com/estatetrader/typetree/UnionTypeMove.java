package com.estatetrader.typetree;

import java.util.List;

public interface UnionTypeMove extends TypePathMultiWayMove {
    List<PossibleSpan> possibleSpans();
}
