package com.estatetrader.objtree;

class NodeContext {
    public final Integer index;
    public final Object entryKey;
    public final String fieldName;

    public NodeContext(Integer index, Object entryKey, String fieldName) {
        this.index = index;
        this.entryKey = entryKey;
        this.fieldName = fieldName;
    }
}
