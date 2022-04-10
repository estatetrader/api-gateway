package com.estatetrader.rule;

import java.io.Closeable;
import java.util.Map;

/**
 * used to listen the watched resource
 * depending on a buffer, the methods here can archive great performance
 */
public interface WatchedResourceListener<T> extends Closeable {

    /**
     * 获取当前节点的数据
     * @return 当前数据
     */
    Object getData();

    /**
     * Check if contains.
     *
     * @param key the key to check
     * @return if the key is contained in the resource
     */
    default boolean containsKey(Object key) {
        return key != null && containsKey(String.valueOf(key));
    }

    /**
     * Check if contains.
     *
     * @param key the key to check
     * @return if the key is contained in the resource
     */
    boolean containsKey(String key);

    /**
     * the keys saved is a set of prefixes
     *
     * @param key key to check
     * @return if the key is a prefix of one of the resource-item
     */
    boolean prefixContainsKey(String key);

    /**
     * return a list which only containing keys that are contained in the resource
     * @param keys keys to filter (read only)
     * @return filtered keys (new list)
     */
    Iterable<String> filter(Iterable<String> keys);

    /**
     * Get data by the key. Returns null if no such key is found.
     *
     * @param key key
     * @return the data related with the key
     */
    T get(String key);

    /**
     * retrieve all keys in the resource
     * @return key list
     */
    Iterable<String> getKeys();

    /**
     * retrieve all values in the resource
     * @return value list
     */
    Iterable<T> getValues();

    /**
     * retrieve all key-value pairs
     * @return key-value pairs
     */
    Iterable<Map.Entry<String, T>> getEntries();

    /**
     * return a nested listener of watched-resource for each child node which is required to watch too.
     *
     * @param childKey the key of the child
     * @return a watched resource of that child
     */
    <C> WatchedResourceListener<C> child(String childKey);

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     */
    @Override
    void close();
}
