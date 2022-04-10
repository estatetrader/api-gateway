package com.estatetrader.objtree;

public interface ObjectTreeInspectorContext<R> {
    ObjectTreeNode parent();
    R siblingReport();
    R descend(ObjectTreeInspector<R> inspector);
}
