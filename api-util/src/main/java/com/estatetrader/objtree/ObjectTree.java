package com.estatetrader.objtree;

public interface ObjectTree<P> {
    <R> R inspect(ObjectTreeInspector<R> inspector);
    void visit(Object rootObj, P param);
}
