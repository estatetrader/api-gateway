package com.estatetrader.objtree;

import com.estatetrader.generic.GenericType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;

public abstract class ObjectTreeNode {

    static final Logger LOGGER = LoggerFactory.getLogger(ObjectTreeNode.class);

    private GenericType type;
    private NodeHandler<Object, Object> handler;

    public GenericType getType() {
        return type;
    }

    <R> R inspect(ObjectTreeInspector<R> inspector, R prev, RouteRecorder recorder) {
        if (recorder.enter(this)) {
            R report = doInspect(inspector, prev, recorder);
            recorder.exit();
            return report;
        } else {
            return null;
        }
    }

    void accept(Object object, Object param, NodeContext nodeContext, RouteRecorder recorder) {
        if (object != null && recorder.enter(object)) {
            doAccept(object, param, nodeContext, recorder);
            recorder.exit();
        }
    }

    PurgeResult purge(Deque<ObjectTreeNode> path) {
        if (path.contains(this)) {
            return new PurgeResult(this);
        } else {
            path.push(this);
            PurgeResult init = handler != null ? PurgeResult.NOT_EMPTY : PurgeResult.EMPTY;
            PurgeResult childrenResult = purgeChildren(path);
            if (path.pop() != this) throw new IllegalStateException("bug");
            return init.merge(childrenResult).upward(this);
        }
    }

    void setType(GenericType type) {
        this.type = type;
    }

    void setHandler(NodeHandler<Object, Object> handler) {
        this.handler = handler;
    }

    public NodeHandler<?, ?> getHandler() {
        return handler;
    }

    protected abstract PurgeResult purgeChildren(Deque<ObjectTreeNode> path);

    protected <R> R doInspect(ObjectTreeInspector<R> inspector, R sibling, RouteRecorder recorder) {
        ObjectTreeInspectorContext<R> context = new ObjectTreeInspectorContext<R>() {
            @Override
            public ObjectTreeNode parent() {
                return ObjectTreeNode.this;
            }
            @Override
            public R siblingReport() {
                return sibling;
            }
            @Override
            public R descend(ObjectTreeInspector<R> inspector) {
                return inspectChildren(inspector, recorder);
            }
        };
        return inspector.inspect(this, context);
    }

    protected abstract <R> R inspectChildren(ObjectTreeInspector<R> inspector, RouteRecorder recorder);

    protected void doAccept(Object object, Object param, NodeContext nodeContext, RouteRecorder recorder) {
        if (handler == null) {
            childrenAccept(object, param, recorder);
            return;
        }

        NodeHandlerContext context = new NodeHandlerContext() {
            @Override
            public Integer index() {
                return nodeContext != null ? nodeContext.index : null;
            }

            @Override
            public Object entryKey() {
                return nodeContext != null ? nodeContext.entryKey : null;
            }

            @Override
            public String fieldName() {
                return nodeContext != null ? nodeContext.fieldName : null;
            }

            @Override
            public ObjectTreeNode node() {
                return ObjectTreeNode.this;
            }
            @Override
            public void descend() {
                childrenAccept(object, param, recorder);
            }
        };
        handler.handle(object, param, context);
    }

    protected abstract void childrenAccept(Object object, Object param, RouteRecorder recorder);
}
