package com.estatetrader.objtree;

import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.typetree.TypePath;

public interface NodeHandlerFactory<K, V, P> {
    K handlerKey(TypePath path, RecordTypeResolver resolver);
    NodeHandler<V, P> createHandler(K handlerKey, RecordTypeResolver resolver);
}
