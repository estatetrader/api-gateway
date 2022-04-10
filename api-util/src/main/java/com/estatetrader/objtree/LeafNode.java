package com.estatetrader.objtree;

import java.util.Deque;

public class LeafNode extends ObjectTreeNode {
    @Override
    protected PurgeResult purgeChildren(Deque<ObjectTreeNode> path) {
        return PurgeResult.EMPTY;
    }

    @Override
    protected <R> R inspectChildren(ObjectTreeInspector<R> inspector, RouteRecorder recorder) {
        return null;
    }

    @Override
    protected void childrenAccept(Object object, Object param, RouteRecorder recorder) {
    }
}
