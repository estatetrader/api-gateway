package com.estatetrader.objtree;

import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.typetree.TypePath;

public class PointcutNodeHandlerProvider<V, P> implements NodeHandlerProvider<V, P> {

    private final NodePointcut pointcut;
    private final NodeHandler<V, P> handler;

    public PointcutNodeHandlerProvider(NodePointcut pointcut, NodeHandler<V, P> handler) {
        this.pointcut = pointcut;
        this.handler = handler;
    }

    @Override
    public NodeHandler<V, P> handlerFor(TypePath path, RecordTypeResolver resolver) {
        return pointcut.matches(path) ? handler : null;
    }
}
