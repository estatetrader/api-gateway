package com.estatetrader.rule;

import com.estatetrader.functions.AnyToLongFunction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * used to manage watched resource, including reading and writing, without buffering
 */
public interface WatchedResourceManager<T> {

    /**
     * create the root node
     */
    void createRoot();

    /**
     * 获取当前节点的数据
     * @return 当前数据
     */
    Object getData();

    /**
     * 设置当前节点的数据
     * @param data 需要设置的值
     */
    void setData(Object data);

    /**
     * Check if contains.
     *
     * @param key the key to check
     * @return if the key is contained in the resource
     */
    default boolean containsKey(Object key) {
        return containsKey(String.valueOf(key));
    }

    /**
     * Check if contains.
     *
     * @param key the key to check
     * @return if the key is contained in the resource
     */
    boolean containsKey(String key);

    /**
     * Get data by the key. Returns null if no such key is found.
     *
     * @param key key
     * @return the data related with the key
     */
    T get(String key);

    /**
     * Get the time left before the entire node been removed
     *
     * @param key the key of the node
     * @return the time left before expired in milliseconds,
     * return -2 if the node does not exists, or
     * return -1 if the node does not expire
     */
    long getLeftTime(String key);

    /**
     * retrieve all keys
     * @return key list
     */
    Iterable<String> getKeys();

    /**
     * return a nested manager of watched-resource for each child node which is required to watch too.
     *
     * @param childKey the key of the child
     * @return a watched resource of that child
     */
    <C> WatchedResourceManager<C> child(String childKey);

    /**
     * get all key / value pairs
     * @return the whole data
     */
    Map<String, T> dump();

    /**
     * Put an item into the resource
     * @param key the key of this item
     */
    default void put(String key) {
        put(key, null);
    }

    /**
     * Put an item into the resource
     * @param key the key of this item
     * @param data an optional data
     */
    default void put(String key, T data) {
        put(key, data, 0);
    }

    /**
     * Put an item into the resource
     * @param key the key of this item
     * @param data an optional data
     * @param ttl how long this node should live in milliseconds (0 means never expire)
     */
    void put(String key, T data, long ttl);

    /**
     * put all keys
     * @param keys keys to put
     */
    default void putAll(Iterable<String> keys) {
        putAll(keys, key -> 0, key -> null);
    }

    /**
     * put all keys
     * @param keys keys to put
     * @param dataProducer a function used to produce data for each key
     */
    default void putAll(Iterable<String> keys, Function<String, T> dataProducer) {
        putAll(keys, key -> 0, dataProducer);
    }

    /**
     * put all keys
     * @param keys keys to put
     * @param ttlProducer a function used to produce ttl for each key
     */
    @SuppressWarnings("unused")
    default void putAll(Iterable<String> keys, AnyToLongFunction<String> ttlProducer) {
        putAll(keys, ttlProducer, key -> null);
    }

    /**
     * put all keys
     * @param keys keys to put
     * @param ttlProducer a function used to produce ttl for each key
     * @param dataProducer a function used to produce data for each key
     */
    void putAll(Iterable<String> keys, AnyToLongFunction<String> ttlProducer, Function<String, T> dataProducer);

    /**
     * replace all keys
     * @param keys keys to replace
     */
    default void replace(Iterable<String> keys) {
        replace(keys, key -> 0, key -> null);
    }

    /**
     * replace all keys
     * @param keys keys to replace
     * @param dataProducer a function used to produce data for each key
     */
    default void replace(Iterable<String> keys, Function<String, T> dataProducer) {
        replace(keys, key -> 0, dataProducer);
    }

    /**
     * replace all keys
     * @param keys keys to replace
     * @param ttlProducer a function used to produce ttl for each key
     */
    default void replace(Iterable<String> keys, AnyToLongFunction<String> ttlProducer) {
        replace(keys, ttlProducer, key -> null);
    }

    /**
     * replace all keys
     * @param keys keys to replace
     * @param ttlProducer a function used to produce ttl for each key
     * @param dataProducer a function used to produce data for each key
     */
    void replace(Iterable<String> keys, AnyToLongFunction<String> ttlProducer, Function<String, T> dataProducer);

    /**
     * Remove a key from the resource
     *
     * @param key of the item
     * @return return true if the key exists
     */
    boolean remove(String key);

    /**
     * async remove the node of the given key from the resource
     * @param key of the item
     * @return return true if the key exists (successfully deleted)
     */
    CompletableFuture<Boolean> asyncRemove(String key);

    /**
     * Remove a key and all it's children
     * @param key the key of the resource to remove
     * @return return true if the key exists
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean removeRecursively(String key);

    /**
     * remove all keys
     * @param keys keys to remove
     */
    @SuppressWarnings("unused")
    void removeAll(Iterable<String> keys);

    /**
     * remove all keys and their children
     * @param keys keys to remove
     */
    void removeAllRecursively(Iterable<String> keys);
}
