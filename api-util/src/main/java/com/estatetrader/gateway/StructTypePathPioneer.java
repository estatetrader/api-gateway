package com.estatetrader.gateway;

import com.estatetrader.typetree.AbstractTypePathPioneer;

public class StructTypePathPioneer extends AbstractTypePathPioneer {
    public StructTypePathPioneer() {
        super(new StructTypeResolver());
    }
}
