package com.estatetrader.objtree;

import com.estatetrader.generic.ArrayType;

import java.lang.reflect.Array;
import java.util.Deque;

public class ArrayNode extends ObjectTreeNode {
    private ObjectTreeNode element;

    void setElement(ObjectTreeNode element) {
        this.element = element;
    }

    @Override
    public ArrayType getType() {
        return (ArrayType) super.getType();
    }

    @Override
    protected PurgeResult purgeChildren(Deque<ObjectTreeNode> path) {
        if (element != null) {
            PurgeResult result = element.purge(path);
            if (result.isEmpty()) {
                element = null;
            }
            return result;
        } else {
            return PurgeResult.EMPTY;
        }
    }

    @Override
    protected <R> R inspectChildren(ObjectTreeInspector<R> inspector, RouteRecorder recorder) {
        if (element != null) {
            return element.inspect(inspector, null, recorder);
        } else {
            return null;
        }
    }

    @Override
    protected void childrenAccept(Object object, Object param, RouteRecorder recorder) {
        if (element == null) {
            return;
        }
        for (int i = 0, len = Array.getLength(object); i < len; i++) {
            NodeContext context = new NodeContext(i, null, null);
            element.accept(Array.get(object, i), param, context, recorder);
        }
    }
}
