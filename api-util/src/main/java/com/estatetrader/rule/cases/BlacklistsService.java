package com.estatetrader.rule.cases;

import com.estatetrader.rule.zk.SimpleZKWatchedResource;
import com.estatetrader.util.ZKOperator;
import com.estatetrader.rule.WatchedResourceManager;
import com.estatetrader.rule.WatchedResourceListener;

import java.io.Closeable;

public class BlacklistsService {

    private static final String ROOT_PATH = "/api/blacklist";

    private final SimpleZKWatchedResource userBlacklist;
    private final SimpleZKWatchedResource deviceBlacklist;
    private final SimpleZKWatchedResource ipBlacklist;
    private final SimpleZKWatchedResource phonePrefixBlacklist;

    public BlacklistsService(ZKOperator operator) {
        userBlacklist = new SimpleZKWatchedResource(ROOT_PATH + "/user", operator);
        deviceBlacklist = new SimpleZKWatchedResource(ROOT_PATH + "/device", operator);
        ipBlacklist = new SimpleZKWatchedResource(ROOT_PATH + "/ip", operator);
        phonePrefixBlacklist = new SimpleZKWatchedResource(ROOT_PATH + "/phone-prefix", operator);
    }

    public Managers createManagers() {
        return new Managers(this);
    }

    public static class Managers {
        public final WatchedResourceManager userBlacklist;
        public final WatchedResourceManager deviceBlacklist;
        public final WatchedResourceManager ipBlacklist;
        public final WatchedResourceManager phonePrefixBlacklist;

        private Managers(BlacklistsService service) {
            this.userBlacklist = service.userBlacklist.createManager();
            this.deviceBlacklist = service.deviceBlacklist.createManager();
            this.ipBlacklist = service.ipBlacklist.createManager();
            this.phonePrefixBlacklist = service.phonePrefixBlacklist.createManager();
        }

        public void createRoot() {
            userBlacklist.createRoot();
            deviceBlacklist.createRoot();
            ipBlacklist.createRoot();
            phonePrefixBlacklist.createRoot();
        }
    }

    public Listeners createListeners() {
        return new Listeners(this);
    }

    public static class Listeners implements Closeable {
        public final WatchedResourceListener<?> userBlacklist;
        public final WatchedResourceListener<?> deviceBlacklist;
        public final WatchedResourceListener<?> ipBlacklist;
        public final WatchedResourceListener<?> phonePrefixBlacklist;

        private Listeners(BlacklistsService service) {
            this.userBlacklist = service.userBlacklist.createListener();
            this.deviceBlacklist = service.deviceBlacklist.createListener();
            this.ipBlacklist = service.ipBlacklist.createListener();
            this.phonePrefixBlacklist = service.phonePrefixBlacklist.createListener();
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
            userBlacklist.close();
            deviceBlacklist.close();
            ipBlacklist.close();
            phonePrefixBlacklist.close();
        }
    }
}
