package com.estatetrader.rule.zk;

import com.estatetrader.functions.AnyToLongFunction;
import com.estatetrader.util.Lambda;
import com.estatetrader.util.ZKOperator;
import com.estatetrader.rule.WatchedResourceManager;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * zk数据的管理类，用于方便修改维护zk的节点和其子节点
 *
 * 主要用于为ZKWatchedResourceListener提供数据
 *
 * 注意：本类也提供了zk的读取功能，但与ZKWatchedResourceListener不同的是，本类不提供本地缓存，每次读取均会访问zk，因此读取的效率不高。
 * 如果有频繁的读取需求，请使用ZKWatchedResourceListener
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
public class ZKWatchedResourceManager<T> implements WatchedResourceManager<T>, ResourceKeySupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKWatchedResourceManager.class);

    private final String path;
    private final ZKOperator operator;
    private final Class<?> dataType;
    private final Class<T> childDataType;
    private final Class<?>[] grandChildDataTypes;

    /**
     * 创建一个类似于分布式集合（Set）的两层资源管理器，其中Set的各元素为子节点的名称
     *
     * @param path 要管理的节点的路径
     * @param operator ZK连接
     */
    @SuppressWarnings("unused")
    public ZKWatchedResourceManager(String path, ZKOperator operator) {
        this(path, operator, null);
    }

    /**
     * 创建一个类似于分布式哈希表（Map）的两层资源管理器
     * 其中Map的key为子节点的名称，Map的value为子节点的数据，其类型由childDataType指定
     *
     * @param path 要管理的节点的路径
     * @param operator ZK连接
     * @param childDataType 要管理的子节点数据类型，为null表示不管理子节点数据，只管理子节点的集合
     */
    public ZKWatchedResourceManager(String path, ZKOperator operator, Class<T> childDataType) {
        this(path, operator, null, childDataType);
    }

    /**
     * 创建一个多层资源管理器
     *
     * @param path 要管理的节点的路径
     * @param operator ZK连接
     * @param dataType 要管理的根节点数据类型，为null表示不管理其数据
     * @param childDataType 要管理的子节点数据类型，为null表示不管理子节点数据，只管理子节点的集合
     * @param grandChildDataTypes 要管理的更深层的节点数据类型，数组长度表示要管理的节点深度，
     *                            数组元素可以为null，表示不管理具体数据，只管理节点
     */
    public ZKWatchedResourceManager(String path,
                                    ZKOperator operator,
                                    Class<?> dataType,
                                    Class<T> childDataType,
                                    Class<?>... grandChildDataTypes) {
        this.path = path;
        this.operator = operator;
        this.dataType = dataType;
        this.childDataType = childDataType;
        this.grandChildDataTypes = grandChildDataTypes;
    }

    /**
     * create the root node
     */
    @Override
    public void createRoot() {
        try {
            operator.recursiveCreateNode(path);
        } catch (KeeperException e) {
            throw new IllegalStateException("create node " + path + " failed!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取当前节点的数据
     *
     * @return 当前数据
     */
    @Override
    public Object getData() {
        if (dataType == null) {
            throw new IllegalStateException("data type of " + path + " is not set");
        }

        try {
            ItemData<?> data = getItemIfExist(path, dataType);
            return data != null ? data.value : null;
        } catch (KeeperException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * 设置当前节点的数据
     *
     * @param data 需要设置的值
     */
    @Override
    public void setData(Object data) {
        ItemData<Object> item = createItem(data, 0, new Date().getTime());
        byte[] bytes = item.serialize();

        try {
            operator.getConn().setData(path, bytes, -1);
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if contains.
     *
     * @param key the key to check
     * @return if the key is contained in the resource
     */
    @Override
    public boolean containsKey(String key) {
        return operator.exists(getChildPath(key));
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
        try {
            ItemData<T> item = getChildItemIfExist(key);
            return item != null ? item.value : null;
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        }
    }

    /**
     * Get the time left before the entire node been removed
     *
     * @param key the key of the node
     * @return the time left before expired in milliseconds,
     * return -2 if the node does not exists, or
     * return -1 if the node does not expire
     */
    @Override
    public long getLeftTime(String key) {
        try {
            ItemData<T> item = getChildItemIfExist(key);
            // a node without data is considered as a node living forever
            if (item == null || item.timeToLive == 0) {
                return -1;
            }
            // a already expired node considered as having 0 zero left time
            return Math.max(System.currentTimeMillis() - (item.timestamp + item.timeToLive), 0);
        } catch (KeeperException.NoNodeException e) {
            return -2;
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        }
    }

    /**
     * retrieve all keys
     *
     * @return key list
     */
    @Override
    public Iterable<String> getKeys() {
        try {
            return getChildren();
        } catch (KeeperException.NoNodeException e) {
            return Collections.emptyList(); // root node is not created, only return an empty list
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        }
    }

    /**
     * return a nested manager of watched-resource for each child node
     *
     * @param childKey the key of the child
     * @return a watched resource of that child
     */
    @Override
    public <C> WatchedResourceManager<C> child(String childKey) {
        Class<C> grandChildDataType;
        Class<?>[] grandGrandChildDataTypes;
        if (grandChildDataTypes.length > 0) {
            //noinspection unchecked
            grandChildDataType = (Class<C>) grandChildDataTypes[0];
            grandGrandChildDataTypes = Arrays.copyOfRange(grandChildDataTypes, 1, grandChildDataTypes.length);
        } else {
            grandChildDataType = null;
            grandGrandChildDataTypes = new Class[0];
        }

        return new ZKWatchedResourceManager<>(getChildPath(childKey), operator,
            childDataType, grandChildDataType, grandGrandChildDataTypes);
    }

    /**
     * get all key / value pairs
     *
     * @return the whole data
     */
    @Override
    public Map<String, T> dump() {
        try {
            Map<String, T> map = new HashMap<>();
            for (String key : getKeys()) {
                ItemData<T> item = getChildItemIfExist(key);
                if (item != null && item.value != null) {
                    map.put(key, item.value);
                }
            }

            return map;
        } catch (KeeperException.NoNodeException e) {
            // root node is not created, so what we should do is just returning an empty map
            return Collections.emptyMap();
        } catch (KeeperException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        }
    }

    private ItemData<Object> createItem(AnyToLongFunction<String> ttlProducer, Function<String, T> dataProducer, long now, String key) {
        return createItem(dataProducer.apply(key), ttlProducer.apply(key), now);
    }

    private ItemData<Object> createItem(Object data, long ttl, long now) {
        ItemData<Object> item = new ItemData<>();
        item.value = data;
        item.timestamp = now;
        item.timeToLive = ttl;
        item.refreshData();
        return item;
    }

    /**
     * Put an item into the resource
     *
     * @param key  the key of this item
     * @param data an optional data
     * @param ttl  how long this node should live in milliseconds (0 means never expire)
     */
    @Override
    public void put(String key, T data, long ttl) {
        ItemData<Object> item = createItem(data, ttl, new Date().getTime());
        byte[] bytes = item.serialize();

        String childPath = getChildPath(key);
        ZooKeeper zk = operator.getConn();
        try {
            if (zk.exists(childPath, false) == null) {
                zk.create(childPath, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                zk.setData(childPath, bytes, -1);
            }
        } catch (KeeperException.NodeExistsException e) {
            LOGGER.warn("Node does already exists. It may be because another process created the node {} " +
                "at the same time.", childPath);

            try {
                zk.setData(childPath, bytes, -1);
            } catch (KeeperException ex) {
                throw new IllegalStateException(e.getMessage(), e);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * put all keys
     *
     * @param keys         keys to put
     * @param ttlProducer  a function used to produce ttl for each key
     * @param dataProducer a function used to produce data for each key
     */
    @Override
    public void putAll(Iterable<String> keys, AnyToLongFunction<String> ttlProducer, Function<String, T> dataProducer) {
        ZooKeeper zk = operator.getConn();
        List<Op> ops = new ArrayList<>();

        long now = new Date().getTime();
        try {
            Set<String> exist = new HashSet<>(getChildren());

            for (String key : keys) {
                ItemData<Object> item = createItem(ttlProducer, dataProducer, now, key);
                byte[] bytes = item.serialize();

                if (exist.contains(key)) {
                    ops.add(Op.setData(getChildPath(key), bytes, -1));
                } else {
                    ops.add(Op.create(getChildPath(key), bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
                }
            }

            zk.multi(ops);
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * replace all keys
     *
     * @param keys         keys to replace
     * @param ttlProducer  a function used to produce ttl for each key
     * @param dataProducer a function used to produce data for each key
     */
    @Override
    public void replace(Iterable<String> keys, AnyToLongFunction<String> ttlProducer, Function<String, T> dataProducer) {
        ZooKeeper zk = operator.getConn();
        List<Op> ops = new ArrayList<>();

        long now = new Date().getTime();
        try {
            Set<String> exist = new HashSet<>(getChildren());

            for (String key : keys) {
                ItemData<Object> item = createItem(ttlProducer,  dataProducer, now, key);
                byte[] bytes = item.serialize();

                if (exist.contains(key)) {
                    ops.add(Op.setData(getChildPath(key), bytes, -1));
                    exist.remove(key); // remove it so afterward we can query exist to see those who should be deleted
                } else {
                    ops.add(Op.create(getChildPath(key), bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
                }
            }

            for (String key : exist) {
                ops.add(Op.delete(getChildPath(key), -1));
            }

            zk.multi(ops);
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Remove a key from the resource
     *
     * @param key of the item
     * @return return true if the key exists
     */
    @Override
    public boolean remove(String key) {
        String childPath = getChildPath(key);
        ZooKeeper zk = operator.getConn();
        try {
            if (zk.exists(childPath, false) == null) {
                return false;
            }

            zk.delete(childPath, -1);
            return true;
        } catch (KeeperException.NoNodeException e) {
            LOGGER.warn("Node is already removed. It may be because another process deleted the znode {} " +
                "at the same time.", childPath);
            return false;
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        }
    }

    /**
     * async remove the node of the given key from the resource
     *
     * @param key of the item
     * @return return true if the key exists (successfully deleted)
     */
    @Override
    public CompletableFuture<Boolean> asyncRemove(String key) {
        String childPath = getChildPath(key);
        ZooKeeper zk = operator.getConn();

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        zk.delete(childPath, -1, (rc, p, c) -> {
            if (KeeperException.Code.OK.intValue() == rc) {
                future.complete(true);
            } else if (KeeperException.Code.NONODE.intValue() == rc) {
                future.complete(false);
            } else {
                future.completeExceptionally(KeeperException.create(KeeperException.Code.get(rc)));
            }
        }, null);

        return future;
    }

    private void depthFirstVisit(String path, Consumer<String> visitor) throws KeeperException, InterruptedException {
        for (String child : operator.getConn().getChildren(path, null)) {
            depthFirstVisit(path + "/" + child, visitor);
        }
        visitor.accept(path);
    }

    /**
     * Remove a key and all it's children
     *
     * @param key the key of the resource to remove
     * @return return true if the key exists
     */
    @Override
    public boolean removeRecursively(String key) {
        try {
            ZooKeeper zk = operator.getConn();
            String childPath = getChildPath(key);

            if (zk.exists(childPath, false) == null) {
                return false;
            }

            List<Op> ops = new ArrayList<>();
            depthFirstVisit(childPath, p -> ops.add(Op.delete(p, -1)));

            zk.multi(ops);
            return true;
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        }
    }

    /**
     * remove all keys
     *
     * @param keys keys to remove
     */
    @Override
    public void removeAll(Iterable<String> keys) {
        try {
            List<Op> ops = new ArrayList<>();

            Set<String> exist = new HashSet<>(getChildren());
            for (String key : keys) {
                if (exist.contains(key)) {
                    ops.add(Op.delete(getChildPath(key), -1));
                }
            }

            operator.getConn().multi(ops);
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * remove all keys and their children
     *
     * @param keys keys to remove
     */
    @Override
    public void removeAllRecursively(Iterable<String> keys) {
        try {
            List<Op> ops = new ArrayList<>();

            Set<String> exist = new HashSet<>(getChildren());
            for (String key : keys) {
                if (exist.contains(key)) {
                    depthFirstVisit(getChildPath(key), p -> ops.add(Op.delete(p, -1)));
                }
            }

            operator.getConn().multi(ops);
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getChildPath(String key) {
        return path + "/" + encodeKey(key);
    }

    private List<String> getChildren() throws KeeperException, InterruptedException {
        List<String> list = operator.getConn().getChildren(path, false);
        return Lambda.map(list, this::decodeKey);
    }

    private ItemData<T> getChildItemIfExist(String key) throws KeeperException, InterruptedException {
        return getItemIfExist(getChildPath(key), childDataType);
    }

    private <D> ItemData<D> getItemIfExist(String nodePath, Class<D> clazz) throws KeeperException, InterruptedException {
        try {
            byte[] bytes = operator.getConn().getData(nodePath, false, null);
            return ItemData.deserialize(bytes, clazz);
        } catch (KeeperException.NoNodeException e) {
            // ignore, if by chance the node does not exist which means it may be deleted by another person
            return null;
        }
    }
}
