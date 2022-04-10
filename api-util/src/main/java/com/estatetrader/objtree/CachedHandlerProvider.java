package com.estatetrader.objtree;

import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.typetree.TypePath;

import java.util.HashMap;
import java.util.Map;

public class CachedHandlerProvider<K, V, P> implements NodeHandlerProvider<V, P> {

    private final NodeHandlerFactory<K, V, P> factory;
    private final Map<K, CacheEntry> cache = new HashMap<>();

    public CachedHandlerProvider(NodeHandlerFactory<K, V, P> factory) {
        this.factory = factory;
    }

    @Override
    public NodeHandler<V, P> handlerFor(TypePath path, RecordTypeResolver resolver) {
        K handlerKey = factory.handlerKey(path, resolver);
        if (handlerKey == null) {
            return null;
        }

        // 防止重复创建
        return cache.computeIfAbsent(handlerKey,
            // 使用一层wrapper可以缓存null handler
            key -> new CacheEntry(factory.createHandler(key, resolver))
        ).handler;
    }

    private class CacheEntry {
        final NodeHandler<V, P> handler;

        CacheEntry(NodeHandler<V, P> handler) {
            this.handler = handler;
        }
    }
}
