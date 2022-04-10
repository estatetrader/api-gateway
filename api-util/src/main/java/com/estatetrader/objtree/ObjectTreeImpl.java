package com.estatetrader.objtree;

class ObjectTreeImpl<P> implements ObjectTree<P> {

    private final ObjectTreeNode rootNode;
    private final RouteRecorderFactory recorderFactory;

    public ObjectTreeImpl(ObjectTreeNode rootNode, RouteRecorderFactory recorderFactory) {
        this.rootNode = rootNode;
        this.recorderFactory = recorderFactory;
    }

    @Override
    public <R> R inspect(ObjectTreeInspector<R> inspector) {
        return rootNode.inspect(inspector, null, new RouteRecorder.VisitNonRecursively());
    }

    @Override
    public void visit(Object rootObj, P param) {
        rootNode.accept(rootObj, param, null, recorderFactory.create());
    }
}
