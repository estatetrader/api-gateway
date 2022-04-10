package com.estatetrader.objtree;

import com.estatetrader.typetree.TypePath;

@FunctionalInterface
public interface NodePointcut {
    boolean matches(TypePath path);
}
