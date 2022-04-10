package com.estatetrader.rule.expire;

import com.estatetrader.rule.zk.ZKWatchedResourceListener;
import com.estatetrader.rule.zk.ZKWatchedResourceManager;
import com.estatetrader.util.ZKOperator;
import com.estatetrader.rule.WatchedResourceManager;
import com.estatetrader.rule.WatchedResourceListener;

import java.util.Collections;
import java.util.List;

/**
 * 将需要强制注销
 * 如果满足上述一个条件，网关会中断此次请求，并向客户端返回 USER_TOKEN_ERROR（-360）或指定的原因
 */
public class ExpiredUserTokenService {

    public static final String ROOT_PATH = "/api/expired/user-token";
    public static final String KEY_OF_ALL_USERS = "all";

    private final ZKOperator operator;

    public ExpiredUserTokenService(ZKOperator operator) {
        this.operator = operator;
    }

    public ExpiredUserTokenManager createManager() {
        return new ManagerImpl(operator);
    }

    private static List<UserTokenExpireRule> unwrapRules(UserTokenExpireRulesWrapper wrapper) {
        return wrapper != null && wrapper.getRules() != null ?
            wrapper.getRules() : Collections.emptyList();
    }

    private static UserTokenExpireRulesWrapper wrapRules(List<UserTokenExpireRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        return new UserTokenExpireRulesWrapper(rules);
    }

    private static class ManagerImpl implements ExpiredUserTokenManager {

        private final WatchedResourceManager<UserTokenExpireRulesWrapper> manager;

        ManagerImpl(ZKOperator operator) {
            this.manager = new ZKWatchedResourceManager<>(ROOT_PATH, operator, UserTokenExpireRulesWrapper.class);
        }

        private void verifyRules(List<UserTokenExpireRule> rules) {
            if (rules == null) {
                return;
            }

            long now = System.currentTimeMillis();

            for (UserTokenExpireRule rule : rules) {
                if (rule.getBeforeTime() <= 0 || rule.getBeforeTime() > now) {
                    throw new IllegalArgumentException("rule.beforeTime " +
                        rule.getBeforeTime() + " must be > 0 and <= now!");
                }
            }
        }

        /**
         * 获取针对所有用户的强制过期规则
         *
         * @return 过期规则列表
         */
        @Override
        public List<UserTokenExpireRule> getRulesForAllUsers() {
            return unwrapRules(manager.get(KEY_OF_ALL_USERS));
        }

        /**
         * 设置针对所有用户的强制过期规则
         * <p>
         * 注意：这个函数的影响面非常大，请再三确认你的参数
         * <p>
         * 这个规则主要用于强制下线有问题的token
         *
         * @param rules 需要设置给所有用户的强制过期规则列表
         * @param ttl   过期规则有效期，以毫秒为单位
         */
        @Override
        public void setRulesForAllUsers(List<UserTokenExpireRule> rules, long ttl) {
            verifyRules(rules);
            manager.put(KEY_OF_ALL_USERS, wrapRules(rules), ttl);
        }

        /**
         * 删除针对所有用户的强制过期规则
         */
        @Override
        public void deleteRulesForAllUsers() {
            manager.remove(KEY_OF_ALL_USERS);
        }

        /**
         * 获取针对特定用户的强制过期规则列表
         *
         * @param userId 用户编号
         * @return 强制过期规则列表
         */
        @Override
        public List<UserTokenExpireRule> getRulesForUser(long userId) {
            return unwrapRules(manager.get(String.valueOf(userId)));
        }

        /**
         * 为指定用设置token强制过期规则（覆盖已有规则）
         *
         * @param userId 用户编号
         * @param rules  规则列表
         * @param ttl    过期规则有效期，以毫秒为单位
         */
        @Override
        public void setRulesForUser(long userId, List<UserTokenExpireRule> rules, long ttl) {
            verifyRules(rules);
            manager.put(String.valueOf(userId), wrapRules(rules), ttl);
        }

        /**
         * 删除指定用户的所有token强制过期规则
         *
         * @param userId 用户编号
         */
        @Override
        public void deleteRulesForUser(long userId) {
            manager.remove(String.valueOf(userId));
        }
    }

    public ExpiredUserTokenListener createListener() {
        return new ListenerImpl(operator);
    }

    private static class ListenerImpl implements ExpiredUserTokenListener {

        private final WatchedResourceListener<UserTokenExpireRulesWrapper> listener;

        ListenerImpl(ZKOperator operator) {
            this.listener = new ZKWatchedResourceListener<>(ROOT_PATH, operator, UserTokenExpireRulesWrapper.class);
        }

        /**
         * 查询针对所有用户的强制过期规则
         *
         * @return 过期规则列表
         */
        @Override
        public List<UserTokenExpireRule> getRulesForAllUsers() {
            return unwrapRules(listener.get(KEY_OF_ALL_USERS));
        }

        /**
         * 查询针对特定用户的强制过期规则列表
         *
         * @param userId 用户编号
         * @return 强制过期规则列表
         */
        @Override
        public List<UserTokenExpireRule> getRulesForUser(long userId) {
            return unwrapRules(listener.get(String.valueOf(userId)));
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
            listener.close();
        }
    }
}
