package com.estatetrader.objtree;

public interface NodeHandlerContext {
    Integer index();
    Object entryKey();
    String fieldName();
    ObjectTreeNode node();
    void descend();
}
