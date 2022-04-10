package com.estatetrader.objtree;

@FunctionalInterface
public interface ObjectTreeInspector<R> {
    R inspect(ObjectTreeNode node, ObjectTreeInspectorContext<R> context);
}
