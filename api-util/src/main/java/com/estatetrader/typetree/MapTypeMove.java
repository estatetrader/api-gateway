package com.estatetrader.typetree;

import com.estatetrader.generic.MapType;

public interface MapTypeMove extends TypePathOneWayMove {
    MapType mapType();
    MapSpan next();
}
