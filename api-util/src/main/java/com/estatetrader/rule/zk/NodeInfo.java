package com.estatetrader.rule.zk;

public class NodeInfo<T> {
    public final ItemData<T> data;
    public final int version;

    public NodeInfo(ItemData<T> data, int version) {
        this.data = data;
        this.version = version;
    }
}
