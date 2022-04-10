package com.estatetrader.rule.cases;

import com.estatetrader.rule.zk.SimpleZKWatchedResource;
import com.estatetrader.util.ZKOperator;
import com.estatetrader.rule.WatchedResourceManager;
import com.estatetrader.rule.WatchedResourceListener;

import java.io.Closeable;

public class RequestVerifyCodeService {

    private static final String ROOT_PATH = "/api/request-verify-code";
    private static final String EXCLUDE_ROOT_PATH = ROOT_PATH + "/exclude";

    private final SimpleZKWatchedResource byUser;
    private final SimpleZKWatchedResource byDevice;
    private final SimpleZKWatchedResource byIp;
    private final SimpleZKWatchedResource byPhonePrefix;
    private final SimpleZKWatchedResource excludeByUser;
    private final SimpleZKWatchedResource excludeByDevice;

    public RequestVerifyCodeService(ZKOperator operator) {
        byUser = new SimpleZKWatchedResource(ROOT_PATH + "/user", operator);
        byDevice = new SimpleZKWatchedResource(ROOT_PATH + "/device", operator);
        byIp = new SimpleZKWatchedResource(ROOT_PATH + "/ip", operator);
        byPhonePrefix = new SimpleZKWatchedResource(ROOT_PATH + "/phone-prefix", operator);
        excludeByUser = new SimpleZKWatchedResource(EXCLUDE_ROOT_PATH + "/user", operator);
        excludeByDevice = new SimpleZKWatchedResource(EXCLUDE_ROOT_PATH + "/device", operator);
    }

    public Managers createManagers() {
        return new Managers(this);
    }

    public static class Managers {
        public final WatchedResourceManager<?> byUser;
        public final WatchedResourceManager<?> byDevice;
        public final WatchedResourceManager<?> byIp;
        public final WatchedResourceManager<?> byPhonePrefix;
        public final WatchedResourceManager<?> excludeByUser;
        public final WatchedResourceManager<?> excludeByDevice;

        public Managers(RequestVerifyCodeService service) {
            this.byUser = service.byUser.createManager();
            this.byDevice = service.byDevice.createManager();
            this.byIp = service.byIp.createManager();
            this.byPhonePrefix = service.byPhonePrefix.createManager();
            this.excludeByUser = service.excludeByUser.createManager();
            this.excludeByDevice = service.excludeByDevice.createManager();
        }

        public void createRoot() {
            byUser.createRoot();
            byDevice.createRoot();
            byIp.createRoot();
            byPhonePrefix.createRoot();
            excludeByUser.createRoot();
            excludeByDevice.createRoot();
        }
    }

    public Listeners createListeners() {
        return new Listeners(this);
    }

    public static class Listeners implements Closeable {
        public final WatchedResourceListener<?> byUser;
        public final WatchedResourceListener<?> byDevice;
        public final WatchedResourceListener<?> byIp;
        public final WatchedResourceListener<?> byPhonePrefix;
        public final WatchedResourceListener<?> excludeByUser;
        public final WatchedResourceListener<?> excludeByDevice;

        public Listeners(RequestVerifyCodeService service) {
            this.byUser = service.byUser.createListener();
            this.byDevice = service.byDevice.createListener();
            this.byIp = service.byIp.createListener();
            this.byPhonePrefix = service.byPhonePrefix.createListener();
            this.excludeByUser = service.excludeByUser.createListener();
            this.excludeByDevice = service.excludeByDevice.createListener();
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
            byUser.close();
            byDevice.close();
            byIp.close();
            byPhonePrefix.close();
            excludeByUser.close();
            excludeByDevice.close();
        }
    }
}
