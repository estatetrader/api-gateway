package com.estatetrader.objtree;

import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.typetree.TypePath;

public class CombinedNodeHandlerProvider implements NodeHandlerProvider<Object, Object> {

    private final NodeHandlerProvider<Object, Object> first;
    private final NodeHandlerProvider<Object, Object> second;

    public CombinedNodeHandlerProvider(NodeHandlerProvider<?, ?> first, NodeHandlerProvider<?, ?> second) {
        //noinspection unchecked
        this.first = (NodeHandlerProvider<Object, Object>) first;
        //noinspection unchecked
        this.second = (NodeHandlerProvider<Object, Object>) second;
    }

    @Override
    public NodeHandler<Object, Object> handlerFor(TypePath path, RecordTypeResolver resolver) {
        NodeHandler<Object, Object> firstHandler = first.handlerFor(path, resolver);
        NodeHandler<Object, Object> secondHandler = second.handlerFor(path, resolver);
        if (firstHandler == null) {
            return secondHandler;
        } else if (secondHandler == null) {
            return firstHandler;
        } else {
            throw new IllegalArgumentException("duplicated node handler found for tree path " + path);
        }
    }
}
