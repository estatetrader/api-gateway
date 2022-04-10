package com.estatetrader.util;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZKOperator implements Watcher, Closeable, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZKOperator.class);
    private static final int RETRY_PERIOD_SECONDS = 3;

    private volatile ZooKeeper conn;
    private final CountDownLatch connectedSemaphore;

    private final String connectString;
    private final int sessionTimeout;

    public ZKOperator(String connectString, int sessionTimeout) {
        this.connectString = connectString;
        this.sessionTimeout = sessionTimeout;
        this.connectedSemaphore = new CountDownLatch(1);

        try {
            connect();
        } catch (IOException e) {
            throw new IllegalArgumentException("could not connect to zookeeper " + connectString, e);
        }

        try {
            connectedSemaphore.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (conn == null) {
            throw new IllegalStateException("conn must be initialized in constructor");
        }
    }

    public boolean exists(String path){
        try {
            Stat stat = conn.exists(path, false);
            return stat != null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException();
        } catch (KeeperException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void recursiveCreateNode(String path) throws KeeperException, InterruptedException {
        if (conn.exists(path, false) == null) {
            int index = path.lastIndexOf('/');
            if (index > 0) {
                recursiveCreateNode(path.substring(0, index));
            }
            try {
                conn.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (KeeperException.NodeExistsException e) {
                LOGGER.warn("Node does already exists. It may be because another process created the znode {} " +
                    "at the same time.", path);
            }
        }
    }

    private void connect() throws IOException {
        conn = new ZooKeeper(connectString, sessionTimeout, this);
    }

    private void reconnect() {
        LOGGER.info("start to reconnect....");

        int retries = 0;
        while (true) {
            retries ++;
            try {
                if (!conn.getState().equals(ZooKeeper.States.CLOSED)) {
                    break;
                }

                conn.close();
                LOGGER.warn("zookeeper lost connection, reconnect");
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format(
                        "reconnect to zookeeper failed after %s times retry. will be retry after %ss.",
                        retries, RETRY_PERIOD_SECONDS), e);
                }

                // sleep then retry
                try {
                    TimeUnit.SECONDS.sleep(RETRY_PERIOD_SECONDS);
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public ZooKeeper getConn() {
        return conn;
    }

    @Override
    public void process(WatchedEvent event) {
        Event.KeeperState state = event.getState();

        switch (state) {
            case SyncConnected:
                LOGGER.info("zookeeper SyncConnected.");
                connectedSemaphore.countDown();
                break;
            case Disconnected: // 这时收到断开连接的消息，这里其实无能为力，因为这时已经和ZK断开连接了，只能等ZK再次开启了
                LOGGER.warn("zookeeper Disconnected!");
                break;
            case Expired: //只能重连
                LOGGER.warn("zookeeper Expired!");
                reconnect();
                break;
            default:
                LOGGER.warn("invalid zookeeper state: {}", state);
                break;
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
     */
    @Override
    public void close() {
        ZooKeeper copy = conn;
        if (copy != null) {
            try {
                copy.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
