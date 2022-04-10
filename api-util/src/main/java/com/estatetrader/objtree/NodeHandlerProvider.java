package com.estatetrader.objtree;

import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.typetree.TypePath;

@FunctionalInterface
public interface NodeHandlerProvider<V, P> {
    NodeHandler<V, P> handlerFor(TypePath path, RecordTypeResolver resolver);
}
