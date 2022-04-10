package com.estatetrader.objtree;

import com.estatetrader.generic.MapType;

import java.util.Deque;
import java.util.Map;

public class MapNode extends ObjectTreeNode {

    private ObjectTreeNode valueNode;

    public void setValueNode(ObjectTreeNode valueNode) {
        this.valueNode = valueNode;
    }

    @Override
    public MapType getType() {
        return (MapType) super.getType();
    }

    @Override
    protected PurgeResult purgeChildren(Deque<ObjectTreeNode> path) {
        PurgeResult valueResult;
        if (valueNode != null) {
            valueResult = valueNode.purge(path);
            if (valueResult.isEmpty()) {
                valueNode = null;
            }
            return valueResult;
        } else {
            return PurgeResult.EMPTY;
        }
    }

    @Override
    protected <R> R inspectChildren(ObjectTreeInspector<R> inspector, RouteRecorder recorder) {
        if (valueNode != null) {
            return valueNode.inspect(inspector, null, recorder);
        } else {
            return null;
        }
    }

    @Override
    protected void childrenAccept(Object object, Object param, RouteRecorder recorder) {
        if (valueNode == null) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
            NodeContext context = new NodeContext(null, entry.getKey(), null);
            valueNode.accept(entry.getValue(), param, context, recorder);
        }
    }
}
