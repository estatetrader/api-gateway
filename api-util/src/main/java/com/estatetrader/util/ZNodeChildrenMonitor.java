package com.estatetrader.util;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class ZNodeChildrenMonitor implements AsyncCallback.ChildrenCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZNodeChildrenMonitor.class);

    private final String path;
    private final ZKOperator operator;
    private volatile long zkSessionId;

    protected ZNodeChildrenMonitor(String path, ZKOperator operator) {
        this.path = path;
        this.operator = operator;
        this.zkSessionId = operator.getConn().getSessionId();
    }

    public void watchChildren() {
        watchChildren(null);
    }

    public void watchChildren(CompletableFuture<?> future) {
        operator.getConn().getChildren(path, this::processEvent, this, future);
    }

    public String getPath() {
        return path;
    }

    public ZKOperator getOperator() {
        return operator;
    }

    private void processEvent(WatchedEvent event) {
        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged ||
            event.getState() == Watcher.Event.KeeperState.Expired) {

            watchChildren();
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children) {
        CompletableFuture<?> future = ctx instanceof CompletableFuture ? (CompletableFuture<?>) ctx : null;
        KeeperException.Code code = KeeperException.Code.get(rc);

        if (code == KeeperException.Code.OK) {
            long newSessionId = operator.getConn().getSessionId();
            onChange(path, children, this.zkSessionId != newSessionId);
            this.zkSessionId = newSessionId;
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
     * 子节点发生变化时被调用，用于同步本地缓存，多次onChange之间顺序调用，没有并发问题
     * @param path 发生变化的节点的路径
     * @param childNodeNames 节点最新的子节点名称列表
     * @param sessionChanged 指示连接回话是否发生改变。发生改变意味着我们可能错过了某些通知，因此应清除相关缓存
     */
    protected abstract void onChange(String path, List<String> childNodeNames, boolean sessionChanged);
}
