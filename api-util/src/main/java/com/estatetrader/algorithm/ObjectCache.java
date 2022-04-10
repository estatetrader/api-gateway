package com.estatetrader.algorithm;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * a cached used to help reuse objects
 * @param <T> object type
 */
public class ObjectCache<T> {

    private final int limit;
    private final Supplier<T> create;
    private final UnaryOperator<T> recycle;
    private final ArrayList<T> pool;

    public ObjectCache(int limit, Supplier<T> create, UnaryOperator<T> recycle) {
        this.limit = limit;
        this.create = create;
        this.recycle = recycle;
        this.pool = new ArrayList<>(limit);
    }

    public T acquire() {
        synchronized (pool) {
            if (!pool.isEmpty()) {
                return pool.remove(pool.size() - 1);
            }
        }
        return create.get();
    }

    public void release(T obj) {
        T recycled = recycle.apply(obj);
        if (recycled == null) {
            return;
        }
        synchronized (pool) {
            if (pool.size() < limit) {
                pool.add(recycled);
            }
        }
    }
}
