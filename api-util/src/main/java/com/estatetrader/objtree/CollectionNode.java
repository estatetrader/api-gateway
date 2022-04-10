package com.estatetrader.objtree;

import com.estatetrader.generic.CollectionType;

import java.util.Collection;
import java.util.Deque;

public class CollectionNode extends ObjectTreeNode {
    private ObjectTreeNode element;

    void setElement(ObjectTreeNode element) {
        this.element = element;
    }

    @Override
    public CollectionType getType() {
        return (CollectionType) super.getType();
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
        int i = 0;
        for (Object o : (Collection<?>) object) {
            NodeContext context = new NodeContext(i++, null, null);
            element.accept(o, param, context, recorder);
        }
    }
}
