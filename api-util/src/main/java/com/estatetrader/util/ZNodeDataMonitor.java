package com.estatetrader.util;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public abstract class ZNodeDataMonitor implements AsyncCallback.DataCallback{
    private static final Logger LOGGER = LoggerFactory.getLogger(ZNodeDataMonitor.class);

    private final String path;
    private final ZKOperator operator;

    protected ZNodeDataMonitor(String path, ZKOperator operator) {
        this.path = path;
        this.operator = operator;
    }

    public String getPath() {
        return path;
    }

    protected void watchData() {
        watchData(null);
    }

    protected void watchData(CompletableFuture<?> future) {
        operator.getConn().getData(path, this::processEvent, this, future);
    }

    private void processEvent(WatchedEvent event) {
        if (event.getType() == Watcher.Event.EventType.NodeDataChanged ||
            event.getState() == Watcher.Event.KeeperState.Expired) {

            watchData();
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        CompletableFuture<?> future = ctx instanceof CompletableFuture ? (CompletableFuture<?>) ctx : null;

        KeeperException.Code code = KeeperException.Code.get(rc);
        if (code == KeeperException.Code.OK) {
            LOGGER.debug("node {} data changed.", path);
            try {
                onChange(path, data, stat.getVersion());
            } catch (Exception e) {
                LOGGER.error("failed to handle the data change of path " + path, e);
            }
        }

        //noinspection DuplicatedCode
        if (code == KeeperException.Code.OK || code == KeeperException.Code.NONODE) {
            if (future != null) {
                future.complete(null);
            }
            return;
        }

        LOGGER.warn("invalid path state: {}", rc);
        if (future != null) {
            future.completeExceptionally(KeeperException.create(code));
        }
    }

    /**
     * 在节点数据发生改变时执行，用于同步本地缓存
     * @param path 发生改变的节点的路径
     * @param data 节点的最新数据
     * @param version 节点的当前的版本号（改变之后最新的版本号）
     */
    protected abstract void onChange(String path, byte[] data, int version);
}
