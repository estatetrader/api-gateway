package com.estatetrader.rule.zk;

import com.estatetrader.functions.Pair;
import com.estatetrader.rule.WatchedResourceEventConsumer;
import com.estatetrader.util.Lambda;
import com.estatetrader.util.ZKOperator;
import com.estatetrader.util.ZNodeChildrenMonitor;
import com.estatetrader.util.ZNodeDataMonitor;
import com.estatetrader.rule.WatchedResourceListener;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/**
 * 基于Zookeeper的资源监听器，用于监听zk节点及其子节点（数据）的变化，并维护本地缓存
 * 使用此类可以将zk的访问成本降低到与本地哈希表的字典查询效率水平
 *
 * 本类仅提供zk的读取，不支持修改管理zk数据，如果有写zk的需求，请使用ZKWatchedResourceManager
 *
 * ZKWatchedResourceListener和ZKWatchedResourceManager应配对使用，用于两个进程（服务）之间的高效率单向数据交换
 *
 * 用法：
 * 1. 在进程A中使用ZKWatchedResourceManager向zk中写入数据，可以是一层（只有一个节点），也可以是多层（有父子节点）
 * 2. 在进程B中使用ZKWatchedResourceListener监听此zk，注意，数据类型和节点的路径须和进程A中对应的ZKWatchedResourceManager一致
 * 3. 在进程B中，ZKWatchedResourceListener会实时监听zk的变化，并将最新的zk数据同步到本地缓存中。至此进程B对zk的数据访问可以转换为
 *    对本地缓存的访问，大大提高了读取效率
 *
 * 使用不同的构造函数参数，ZKWatchedResourceListener可以应用于多种使用场景：
 * 1. 简单数据（指只能更新整条数据，不能修改其部分数据）同步，例如功能开关
 * 2. 分布式集合（可以更新集合中的部分数据），例如超级管理员名单
 * 3. 多级分布式集合（可以仅更新某个层级中的部分数据），例如权限树（子系统-API-角色）
 *
 * @param <T> 子节点的数据类型，如果不关注子节点数据，则使用Void作为其类型
 */
public class ZKWatchedResourceListener<T> implements WatchedResourceListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKWatchedResourceListener.class);
    private static final Cleaner cleaner = new Cleaner();

    private final NodeListener root;

    /**
     * 创建一个类似于分布式集合（Set）的两层资源监听器，其中Set的各元素为子节点的名称
     *
     * @param path 要监听的节点的路径
     * @param operator ZK连接
     */
    @SuppressWarnings("unused")
    public ZKWatchedResourceListener(String path, ZKOperator operator) {
        this(path, operator, null);
    }

    /**
     * 创建一个类似于分布式哈希表（Map）的两层资源监听器
     * 其中Map的key为子节点的名称，Map的value为子节点的数据，其类型由childDataType指定
     *
     * @param path 要监听的节点的路径
     * @param operator ZK连接
     * @param childDataType 要监听的子节点数据类型，为null表示不监听子节点数据，只监听子节点的集合
     */
    public ZKWatchedResourceListener(String path, ZKOperator operator, Class<T> childDataType) {
        this(path, operator, null, null, childDataType);
    }

    /**
     * 创建一个类似于分布式哈希表（Map）的两层资源监听器
     * 其中Map的key为子节点的名称，Map的value为子节点的数据，其类型由childDataType指定
     *
     * @param path 要监听的节点的路径
     * @param operator ZK连接
     * @param eventConsumer 可选的事件监听器，用于在zk发生变化时执行一些回调
     * @param childDataType 要监听的子节点数据类型，为null表示不监听子节点数据，只监听子节点的集合
     */
    public ZKWatchedResourceListener(String path,
                                     ZKOperator operator,
                                     WatchedResourceEventConsumer eventConsumer,
                                     Class<T> childDataType) {
        this(path, operator, eventConsumer, null, childDataType);
    }

    /**
     * 创建一个多层资源监听器
     *
     * @param path 要监听的节点的路径
     * @param operator ZK连接
     * @param eventConsumer 可选的事件监听器，用于在zk发生变化时执行一些回调
     * @param dataType 要监听的根节点数据类型，为null表示不监听其数据
     * @param childDataType 要监听的子节点数据类型，为null表示不监听子节点数据，只监听子节点的集合
     * @param grandChildDataTypes 要监听的更深层的节点数据类型，数组长度表示监听深度，
     *                            数组元素可以为null，表示不监听具体数据，只监听节点
     */
    public ZKWatchedResourceListener(String path,
                                     ZKOperator operator,
                                     WatchedResourceEventConsumer eventConsumer,
                                     Class<?> dataType,
                                     Class<T> childDataType,
                                     Class<?>... grandChildDataTypes) {
        this(path, operator, eventConsumer, true, dataType, childDataType, grandChildDataTypes);
    }

    /**
     * 创建一个多层资源监听器
     *
     * @param path 要监听的节点的路径
     * @param operator ZK连接
     * @param eventConsumer 可选的事件监听器，用于在zk发生变化时执行一些回调
     * @param dataType 要监听的根节点数据类型，为null表示不监听其数据
     * @param childDataType 要监听的子节点数据类型，为null表示不监听子节点数据，只监听子节点的集合
     * @param grandChildDataTypes 要监听的更深层的节点数据类型，数组长度表示监听深度，
     *                            数组元素可以为null，表示不监听具体数据，只监听节点
     */
    public ZKWatchedResourceListener(String path,
                                     ZKOperator operator,
                                     WatchedResourceEventConsumer eventConsumer,
                                     boolean waitForReady,
                                     Class<?> dataType,
                                     Class<T> childDataType,
                                     Class<?>... grandChildDataTypes) {
        long timestamp = System.currentTimeMillis();

        try {
            operator.recursiveCreateNode(path);
        } catch (KeeperException e) {
            throw new IllegalStateException("create node " + path + " failed!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }

        Class<?>[] dataTypes = new Class[1 + 1 + grandChildDataTypes.length];
        dataTypes[0] = dataType;
        dataTypes[1] = childDataType;
        System.arraycopy(grandChildDataTypes, 0, dataTypes, 2, grandChildDataTypes.length);

        this.root = new NodeListener(path, operator, dataTypes, eventConsumer);
        if (waitForReady) {
            this.root.waitForReady();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("ResourceListener[{}] has initialized in {}ms", path, System.currentTimeMillis() - timestamp);
            }
        }
    }

    private ZKWatchedResourceListener(NodeListener root) {
        this.root = root;
    }

    /**
     * 获取当前节点的数据
     *
     * @return 当前数据
     */
    @Override
    public Object getData() {
        return root.getValue();
    }

    /**
     * Check if contains.
     * Only be called after watch.
     *
     * @param key the key to check
     * @return if the key is contained in the resource
     */
    @Override
    public boolean containsKey(String key) {
        return root.children.children.containsKey(key);
    }

    /**
     * the keys saved is a set of prefixes
     *
     * @param key key
     * @return exist or not
     */
    @Override
    public boolean prefixContainsKey(String key) {
        for (String prefix : root.children.children.keySet()) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return a list which only containing keys that are contained in the resource
     *
     * @param keys keys to filter (read only)
     * @return filtered keys (new list)
     */
    @Override
    public Iterable<String> filter(Iterable<String> keys) {
        List<String> list = new ArrayList<>();
        for (String key : root.children.children.keySet()) {
            if (containsKey(key)) {
                list.add(key);
            }
        }
        return list;
    }

    /**
     * Get data by the key. Returns null if no such key is found.
     * Only be called after watch.
     *
     * @param key key
     * @return the data related with the key
     */
    @Override
    public T get(String key) {
        NodeListener child = root.children.children.get(key);
        //noinspection unchecked
        return child != null ? (T) child.getValue() : null;
    }

    /**
     * retrieve all keys in the resource
     *
     * @return key list
     */
    @Override
    public Iterable<String> getKeys() {
        return root.children.children.keySet();
    }

    /**
     * retrieve all values in the resource
     *
     * @return value list
     */
    @Override
    public Iterable<T> getValues() {
        Iterator<NodeListener> iter = root.children.children.values().iterator();

        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                //noinspection unchecked
                return (T) iter.next().getValue();
            }
        };
    }

    /**
     * retrieve all key-value pairs
     *
     * @return key-value pairs
     */
    @Override
    public Iterable<Map.Entry<String, T>> getEntries() {
        Iterator<Map.Entry<String, NodeListener>> iter = root.children.children.entrySet().iterator();

        return () -> new Iterator<Map.Entry<String, T>>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * Returns the next element in the iteration.
             *
             * @return the next element in the iteration
             * @throws NoSuchElementException if the iteration has no more elements
             */
            @Override
            public Map.Entry<String, T> next() {
                Map.Entry<String, NodeListener> entry = iter.next();
                //noinspection unchecked
                return new AbstractMap.SimpleEntry<>(entry.getKey(), (T) entry.getValue().getValue());
            }
        };
    }

    /**
     * return a nested listener of watched-resource for each child node which is required to watch too.
     *
     * @param childKey the key of the child
     * @return a watched resource of that child
     */
    @Override
    public <C> WatchedResourceListener<C> child(String childKey) {
        NodeListener child = root.children.children.get(childKey);
        if (child == null) {
            return null;
        }
        return new ZKWatchedResourceListener<>(child);
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     */
    @Override
    public void close() {
        root.close();
    }

    private static void waitForFuture(Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class NodeListener extends ZNodeDataMonitor implements Closeable, ResourceKeySupport {

        private final String key;
        private volatile NodeInfo<?> info;
        private final CompletableFuture<?> readyFuture;
        private final Class<?> dataType;
        private final ChildrenListener children;
        private final WatchedResourceEventConsumer eventConsumer;

        /**
         * 创建一个节点监听器
         *
         * 节点监听器不仅可以监听当前节点的数据和其子节点，也可以用于监听子节点的子节点，以实现对整个节点树的监听
         * 具体用法如下：
         * 1. 如果数组为空，则表示不监听此节点的数据以及此节点的子节点
         * 2. 数组的长度表示节点的监听深度，例如，数组长度为1表示不监听其子节点；数组长度为2表示监听子节点
         * 3. 数组的每个元素可以设置为null，表示不监听在这个深度上的节点的数据，例如0号元素为null表示不监听此节点的数据
         * 4. 是否监听节点数据和是否监听子节点没有必然关系，长度控制是否监听子节点以及子节点的子节点；
         *    数组元素控制是否监听对应深度的节点的数据以及反序列化时使用的数据类型
         *
         * @param path 要监听的节点的路径
         * @param dataTypes 用于控制监听深度和各个深度上的节点数据类型
         * @param eventConsumer 可选的监听器
         */
        NodeListener(String path,
                     ZKOperator operator,
                     Class<?>[] dataTypes,
                     WatchedResourceEventConsumer eventConsumer) {
            super(path, operator);

            int slashIndex = path.lastIndexOf('/');
            if (slashIndex < 0 || slashIndex == path.length() - 1) {
                throw new IllegalArgumentException("invalid node path " + path);
            }
            this.key = decodeKey(path.substring(slashIndex + 1));
            this.eventConsumer = eventConsumer;

            if (dataTypes == null || dataTypes.length == 0) {
                this.dataType = null;
                this.children = null;
                this.readyFuture = null;
            } else {
                this.dataType = dataTypes[0];
                this.readyFuture = new CompletableFuture<>();
                watchData(this.readyFuture);

                if (dataTypes.length > 1) {
                    Class<?>[] nextDataTypes = Arrays.copyOfRange(dataTypes, 1, dataTypes.length);
                    this.children = new ChildrenListener(path, operator, nextDataTypes, eventConsumer);
                    LOGGER.debug("register cleaner for {}", path);
                    cleaner.register(this.children);
                } else {
                    this.children = null;
                }
            }
        }

        void waitForReady() {
            if (readyFuture != null) {
                waitForFuture(readyFuture);
            }

            if (children != null) {
                children.waitForReady();
            }
        }

        void forceUpdate() {
            watchData();
            if (children != null) {
                children.forceUpdate();
            }
        }

        Object getValue() {
            if (dataType == null) {
                throw new IllegalStateException("data type of " + getPath() + " is not set");
            }

            NodeInfo<?> copy = this.info; // 获取节点数据的当前副本（并发）
            return copy != null && copy.data != null ? copy.data.value : null;
        }

        /**
         * 在节点数据发生改变时执行，用于同步本地缓存
         *
         * @param path 发生改变的节点的路径
         * @param data 节点的最新数据
         * @param version 节点的当前的版本号（改变之后最新的版本号）
         */
        @Override
        protected void onChange(String path, byte[] data, int version) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("zk watched resource monitor {} data changed == {}", getPath(),
                    data != null ? new String(data, StandardCharsets.UTF_8) : "");
            }

            ItemData<?> newData = ItemData.deserialize(data, dataType);
            NodeInfo<?> oldInfo = this.info;
            Object oldValue = oldInfo != null && oldInfo.data != null ? oldInfo.data.value : null;

            this.info = new NodeInfo<>(newData, version);

            if (eventConsumer != null) {
                eventConsumer.onChildDataChange(key, oldValue, newData.value);
            }
        }

        /**
         * Closes this stream and releases any system resources associated
         * with it. If the stream is already closed then invoking this
         * method has no effect.
         */
        @Override
        public void close() {
            if (children != null) {
                children.close();
            }
        }
    }

    private static class ChildrenListener extends ZNodeChildrenMonitor
        implements Cleanable, Closeable, ResourceKeySupport {

        private final Class<?>[] dataTypes;
        private final CompletableFuture<?> readyFuture;
        // 使用volatile实现并发读写方案，应注意每次访问此字段的值都将不同，但是map本身应只读，不允许修改
        private volatile Map<String, NodeListener> children;
        private final WatchedResourceEventConsumer eventConsumer;

        ChildrenListener(String path,
                         ZKOperator operator,
                         Class<?>[] dataTypes,
                         WatchedResourceEventConsumer eventConsumer) {
            super(path, operator);
            this.dataTypes = dataTypes;
            this.readyFuture = new CompletableFuture<>();
            this.children = Collections.emptyMap();
            this.eventConsumer = eventConsumer;
            watchChildren(this.readyFuture);
        }

        void waitForReady() {
            waitForFuture(readyFuture);
            for (NodeListener child : children.values()) {
                child.waitForReady();
            }
        }

        void forceUpdate() {
            watchChildren();
        }

        /**
         * 子节点发生变化时被调用，用于同步本地缓存，多次onChange之间顺序调用，没有并发问题
         *
         * @param path           发生变化的节点的路径
         * @param childNodeNames       节点最新的子节点名称列表
         * @param sessionChanged 指示连接回话是否发生改变。发生改变意味着我们可能错过了某些通知，因此应清除相关缓存
         */
        @Override
        protected void onChange(String path, List<String> childNodeNames, boolean sessionChanged) {
            List<String> decodedChildrenName = Lambda.map(childNodeNames, this::decodeKey);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("children of {} has changed to {}", path, String.join(", ", decodedChildrenName));
            }

            Map<String, NodeListener> oldChildren = this.children; // 内存屏障点
            Map<String, NodeListener> newChildren = new HashMap<>(decodedChildrenName.size());

            // 拷贝、创建新增的节点或者在会话变化后强制刷新节点数据
            for (String key : decodedChildrenName) {
                NodeListener child = oldChildren.get(key);
                if (child != null) {
                    newChildren.put(key, child);
                    if (sessionChanged) {
                        child.forceUpdate();
                    }
                } else {
                    newChildren.put(key, createChild(key));
                }
            }

            // 清理已经删除的节点
            for (Map.Entry<String, NodeListener> entry : oldChildren.entrySet()) {
                if (!newChildren.containsKey(entry.getKey())) {
                    entry.getValue().close();
                }
            }

            this.children = newChildren; // 内存屏障点

            if (eventConsumer != null) {
                eventConsumer.onChildrenChange(new ArrayList<>(oldChildren.keySet()),
                    new ArrayList<>(newChildren.keySet()));
            }
        }

        private NodeListener createChild(String key) {
            String childPath = getPath() + "/" + encodeKey(key);
            return new NodeListener(childPath, getOperator(), dataTypes, eventConsumer);
        }

        @Override
        public void cleanup() {
            // [{node path, node version}]
            List<Pair<String, Integer>> nodesToRemove = new LinkedList<>();
            long now = new Date().getTime();

            Map<String, NodeListener> copy = this.children;
            for (Map.Entry<String, NodeListener> entry : copy.entrySet()) {
                NodeInfo<?> info = entry.getValue().info;
                if (info != null && info.data != null &&
                    info.data.timeToLive > 0 && info.data.timestamp + info.data.timeToLive < now) {
                    // timeout is enabled and timed out
                    nodesToRemove.add(new Pair<>(entry.getValue().getPath(), info.version));
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("cleaning up loop was triggered to clean {} out of {} children", nodesToRemove.size(), copy.size());
            }

            int count = 0;
            for (Pair<String, Integer> pair : nodesToRemove) {
                ZooKeeper zk = getOperator().getConn();
                String path = pair.first;
                try {
                    zk.delete(pair.first, pair.second);
                    LOGGER.info("Node {} is cleaned up due to expiration", path);
                } catch (KeeperException.NoNodeException e) {
                    LOGGER.warn("Node is already removed. It may be because another process deleted the znode {} " +
                        "at the same time.", path);
                } catch (KeeperException.NotEmptyException e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Node is not empty. It may be because there is one of the child nodes whose ttl is " +
                            "more than the node {} itself", path);
                    }
                } catch (KeeperException.BadVersionException e) {
                    LOGGER.warn("Node version is not match. It may be because the node {} have been updated " +
                        "after we git it, skipping cleaning up", path);
                } catch (KeeperException e) {
                    LOGGER.error("failed to clean up node {}. retry at next time", path);
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (++count > 10000) {
                    LOGGER.warn("there are too many timed out keys ({}) so that we cannot clean up at the same time. " +
                        "next time it will continue", nodesToRemove.size() - 10000);
                    break;
                }
            }
        }

        /**
         * Closes this stream and releases any system resources associated
         * with it. If the stream is already closed then invoking this
         * method has no effect.
         */
        @Override
        public void close() {
            for (NodeListener child : children.values()) {
                child.close();
            }
            cleaner.unregister(this);
        }
    }

    private interface Cleanable {
        void cleanup();
    }

    private static class Cleaner implements AutoCloseable {

        private static final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(Cleaner::newThread);

        private static Thread newThread(Runnable r) {
            Thread t = new Thread(r, "watched-resource-cleanup-worker");
            t.setDaemon(true);
            return t;
        }

        private final List<Cleanable> list = new ArrayList<>();
        private boolean started = false;

        private void cleanup() {
            synchronized (list) {
                for (Cleanable cleanable : list) {
                    try {
                        cleanable.cleanup();
                    } catch (Exception e) {
                        LOGGER.error("error occurred while cleanup: " + e.getMessage(), e);
                    }
                }
            }
        }

        void register(Cleanable cleanable) {
            synchronized (list) {
                if (!list.contains(cleanable)) {
                    list.add(cleanable);
                }

                if (!started) {
                    // 生产环境多副本部署的情况下，为了避免两台机器同步清理同一组数据导致的冲突，因此引入随机延迟启动
                    // 为了避免干扰启动过程，清理动作将在1分钟之后，并在1-2分钟之间随机产生
                    int delay = new SecureRandom().nextInt(60) + 60;
                    executor.scheduleWithFixedDelay(this::cleanup, delay, 60, TimeUnit.SECONDS);
                    started = true;
                }
            }
        }

        void unregister(Cleanable cleanable) {
            synchronized (list) {
                list.remove(cleanable);
            }
        }

        /**
         * Closes this stream and releases any system resources associated
         * with it. If the stream is already closed then invoking this
         * method has no effect.
         *
         * <p> As noted in {@link AutoCloseable#close()}, cases where the
         * close may fail require careful attention. It is strongly advised
         * to relinquish the underlying resources and to internally
         * <em>mark</em> the {@code Closeable} as closed, prior to throwing
         * the {@code IOException}.
         *
         */
        @Override
        public void close() {
            executor.shutdown();
        }
    }
}
