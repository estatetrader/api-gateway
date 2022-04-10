package com.estatetrader.objtree;

@FunctionalInterface
public interface NodeHandler<V, P> {
    void handle(V value, P param, NodeHandlerContext context);
}
