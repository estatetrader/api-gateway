package com.estatetrader.rule.authorizing;

import com.estatetrader.util.ZKOperator;
import com.estatetrader.rule.WatchedResourceListener;
import com.estatetrader.rule.WatchedResourceManager;
import com.estatetrader.rule.zk.ZKWatchedResourceListener;
import com.estatetrader.rule.zk.ZKWatchedResourceManager;

import java.util.Map;

public class AuthorizationTree {

    private static final String ROOT_PATH = "/api/authorizing";

    private final ZKOperator operator;

    public AuthorizationTree(ZKOperator operator) {
        this.operator = operator;
    }

    public AuthorizationTreeManager createManager() {
        return new ManagerImpl(operator);
    }

    private static class ManagerImpl implements AuthorizationTreeManager {

        private final WatchedResourceManager<SubsystemInfo> manager;

        ManagerImpl(ZKOperator operator) {
            this.manager = new ZKWatchedResourceManager<>(ROOT_PATH, operator,
                null, // 第一层，根节点
                SubsystemInfo.class, // 第二层，子系统节点
                ApiInfo.class, // 第三层，API节点
                null // 第四层，角色节点，不关心其数据结构
            );
        }

        /**
         * 获取子系统授权信息，不包含子节点
         *
         * @param name 子系统名称
         */
        @Override
        public SubsystemInfo getSubsystem(String name) {
            return manager.get(name);
        }

        /**
         * 创建或更新子系统授权信息
         *
         * @param subsystemName 子系统名称
         * @param info          子系统信息
         */
        @Override
        public void putSubsystem(String subsystemName, SubsystemInfo info) {
            manager.put(subsystemName, info);
        }

        /**
         * 删除子系统以及挂载在此子系统下的所有API及其角色
         *
         * @param subsystemName 待删除的子系统名称
         */
        @Override
        public void deleteSubsystem(String subsystemName) {
            if (!manager.containsKey(subsystemName)) {
                return;
            }

            // 考虑到删除子系统的性能问题，我们会先删除该子系统下的每个API，然后再删除子系统节点
            WatchedResourceManager<ApiInfo> apiManager = getApiManager(subsystemName);
            for (String api : apiManager.getKeys()) {
                apiManager.removeRecursively(api);
            }

            manager.removeRecursively(subsystemName);
        }

        private WatchedResourceManager<ApiInfo> getApiManager(String subsystemName) {
            return manager.child(subsystemName);
        }

        /**
         * 获取子系统下挂载的API
         *
         * @param subsystemName 子系统名称
         * @return 子系统权限树下挂载的所有API列表
         */
        @Override
        public Iterable<String> getApis(String subsystemName) {
            return getApiManager(subsystemName).getKeys();
        }

        /**
         * 获取子系统下的某个API的授权信息
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @return API的授权信息
         */
        @Override
        public ApiInfo getApi(String subsystemName, String apiName) {
            return getApiManager(subsystemName).get(apiName);
        }

        /**
         * 获取子系统下挂载的所有API及其授权信息
         *
         * @param subsystemName 子系统名称
         * @return API名称->授权信息表
         */
        @Override
        public Map<String, ApiInfo> getApiInfos(String subsystemName) {
            return getApiManager(subsystemName).dump();
        }

        /**
         * 创建或更新给定子系统下指定API的授权信息
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @param info          API授权信息
         */
        @Override
        public void putApiInfo(String subsystemName, String apiName, ApiInfo info) {
            getApiManager(subsystemName).put(apiName, info);
        }

        /**
         * 创建或更新给定子系统下一组API的授权信息（不删除表中不存在的API授权信息）
         *
         * @param subsystemName 子系统名称
         * @param infoMap       API名称->授权信息表
         */
        @Override
        public void putApiInfos(String subsystemName, Map<String, ApiInfo> infoMap) {
            getApiManager(subsystemName).putAll(infoMap.keySet(), infoMap::get);
        }

        /**
         * 删除子系统下指定的API授权信息及其角色
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         */
        @Override
        public void deleteApi(String subsystemName, String apiName) {
            getApiManager(subsystemName).removeRecursively(apiName);
        }

        /**
         * 删除子系统下指定的一组API授权信息及其角色
         *
         * @param subsystemName 子系统名称
         * @param apis          API名称列表
         */
        @Override
        public void deleteApis(String subsystemName, Iterable<String> apis) {
            getApiManager(subsystemName).removeAllRecursively(apis);
        }

        private WatchedResourceManager<?> getRoleManager(String subsystemName, String apiName) {
            return getApiManager(subsystemName).child(apiName);
        }

        /**
         * 获取子系统的某个API下挂载的所有角色
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @return 挂载的角色列表
         */
        @Override
        public Iterable<String> getApiRoles(String subsystemName, String apiName) {
            return getRoleManager(subsystemName, apiName).getKeys();
        }

        /**
         * 将角色加入到子系统的指定API授权信息中
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @param role          角色
         */
        @Override
        public void addRoleToApi(String subsystemName, String apiName, String role) {
            getRoleManager(subsystemName, apiName).put(role);
        }

        /**
         * 将一组角色加入到子系统的指定API授权信息中
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @param roles         待加入的角色列表
         */
        @Override
        public void addRolesToApi(String subsystemName, String apiName, Iterable<String> roles) {
            getRoleManager(subsystemName, apiName).putAll(roles);
        }

        /**
         * 将一组角色设置为子系统的指定API角色组（会删除掉所有不在roles中出现的角色）
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @param roles         新的角色列表
         */
        @Override
        public void setRolesToApi(String subsystemName, String apiName, Iterable<String> roles) {
            getRoleManager(subsystemName, apiName).replace(roles);
        }

        /**
         * 从子系统的指定API中删除某个角色
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @param role          角色
         */
        @Override
        public void removeRoleFromApi(String subsystemName, String apiName, String role) {
            getRoleManager(subsystemName, apiName).removeRecursively(role);
        }

        /**
         * 从子系统的指定API中删除一组角色
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @param roles         需要删除的角色列表
         */
        @Override
        public void removeRoleFromApis(String subsystemName, String apiName, Iterable<String> roles) {
            getRoleManager(subsystemName, apiName).removeAllRecursively(roles);
        }

        /**
         * 将角色添加到指定子系统的一组API
         *
         * @param subsystemName 子系统名称
         * @param apiNames      被赋予API列表
         * @param role          角色名
         */
        @Override
        public void addRoleToApis(String subsystemName, Iterable<String> apiNames, String role) {
            for (String apiName : apiNames) {
                addRoleToApi(subsystemName, apiName, role);
            }
        }

        /**
         * 将角色从指定子系统的一组API中删除
         *
         * @param subsystemName 子系统名称
         * @param apiNames      被赋予API列表
         * @param role          角色名
         */
        @Override
        public void removeRoleFromApis(String subsystemName, Iterable<String> apiNames, String role) {
            for (String apiName : apiNames) {
                removeRoleFromApi(subsystemName, apiName, role);
            }
        }
    }

    public AuthorizationTreeListener createListener(boolean enabled) {
        return enabled ? new ListenerImpl(operator) : null;
    }

    private static class ListenerImpl implements AuthorizationTreeListener {

        private final WatchedResourceListener<SubsystemInfo> listener;

        ListenerImpl(ZKOperator operator) {
            this.listener = new ZKWatchedResourceListener<>(ROOT_PATH,
                operator,
                null,
                false,
                null, // 第一层，根节点，不监听数据
                SubsystemInfo.class, // 第二层，子系统节点，监听数据
                ApiInfo.class, // 第三层，API节点，监听数据
                null); // 第四层，角色节点，只监听节点，不监听数据
        }

        /**
         * 获取子系统授权信息，不包含子节点
         *
         * @param name 子系统名称
         */
        @Override
        public SubsystemInfo getSubsystem(String name) {
            return listener.get(name);
        }

        private WatchedResourceListener<ApiInfo> getApiListener(String subsystemName) {
            return listener.child(subsystemName);
        }

        /**
         * 获取子系统下的某个API的授权信息
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @return API的授权信息
         */
        @Override
        public ApiInfo getApi(String subsystemName, String apiName) {
            return getApiListener(subsystemName).get(apiName);
        }

        private WatchedResourceListener<?> getRoleListener(String subsystemName, String apiName) {
            return getApiListener(subsystemName).child(apiName);
        }

        /**
         * 检查子系统的某个API是否包含给定的角色
         * 注意：此函数仅用于权限树的结构判断，不用于业务逻辑，实际的校验逻辑需要依赖子系统和API信息
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @param role          角色
         * @return 返回是否包含该角色
         */
        @Override
        public boolean containsApiRole(String subsystemName, String apiName, String role) {
            return getRoleListener(subsystemName, apiName).containsKey(role);
        }

        /**
         * 获取子系统的某个API下挂载的所有角色
         *
         * @param subsystemName 子系统名称
         * @param apiName       API名称
         * @return 挂载的角色列表
         */
        @Override
        public Iterable<String> getApiRoles(String subsystemName, String apiName) {
            return getRoleListener(subsystemName, apiName).getKeys();
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
