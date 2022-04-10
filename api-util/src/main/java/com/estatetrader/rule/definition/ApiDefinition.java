package com.estatetrader.rule.definition;

import com.alibaba.fastjson.JSON;
import com.estatetrader.entity.FileUploadInfo;
import com.estatetrader.util.Md5Util;
import com.estatetrader.util.ZKOperator;
import com.estatetrader.rule.WatchedResourceEventConsumer;
import com.estatetrader.rule.WatchedResourceListener;
import com.estatetrader.rule.zk.ZKWatchedResourceListener;
import com.estatetrader.rule.zk.ZKWatchedResourceManager;

import java.util.*;

public class ApiDefinition {

    public static final String ROOT_PATH = "/api/info";

    private final ZKOperator operator;

    public ApiDefinition(ZKOperator operator) {
        this.operator = operator;
    }

    public ApiDefinitionManager createManager() {
        return new ManagerImpl(operator);
    }

    private static class ManagerImpl implements ApiDefinitionManager {

        private final ZKWatchedResourceManager<ApiInfo> manager;

        ManagerImpl(ZKOperator operator) {
            this.manager = new ZKWatchedResourceManager<>(ROOT_PATH, operator, ApiSummary.class, ApiInfo.class);
        }

        @Override
        public void sync(Collection<ApiInfo> apiList) {
            manager.createRoot();

            // 为了减少IO压力，使用md5预检
            String md5 = Md5Util.computeToBase64(JSON.toJSONBytes(apiList));
            ApiSummary summary = (ApiSummary) manager.getData();
            if (summary != null && md5.equals(summary.getHash())) {
                // zk中已包含最新的API信息，无需同步
                return;
            }

            Map<String, ApiInfo> existingApis = manager.dump();
            Set<String> added = new HashSet<>();

            for (ApiInfo api : apiList) {

                if (!added.add(api.methodName)) {
                    continue; // avoid duplicated api
                }

                ApiInfo old = existingApis.get(api.methodName);
                if (old == null || !old.equals(api)) {
                    manager.put(api.methodName, api);
                }
            }

            for (String api : existingApis.keySet()) {
                if (!added.contains(api)) {
                    manager.remove(api);
                }
            }

            ApiSummary newSummary = summary != null ? summary : new ApiSummary();
            newSummary.setHash(md5);
            manager.setData(newSummary);
        }

        @Override
        public ApiInfo get(String apiName) {
            return manager.get(apiName);
        }
    }

    public ApiDefinitionListener createListener() {
        return new ListenerImpl(operator);
    }

    private static class ListenerImpl implements ApiDefinitionListener {

        private final WatchedResourceListener<ApiInfo> listener;

        ListenerImpl(ZKOperator operator) {
            this.listener = new ZKWatchedResourceListener<>(ROOT_PATH, operator, null, ApiSummary.class, ApiInfo.class);
        }

        @Override
        public List<ApiInfo> getApiInfoList() {
            List<ApiInfo> list = new ArrayList<>();
            for (ApiInfo api : listener.getValues()) {
                list.add(api);
            }
            return list;
        }

        @Override
        public Set<String> subsystemApiList(String subsystem) {
            Set<String> set = new HashSet<>();
            for (ApiInfo api : listener.getValues()) {
                if (Objects.equals(api.subsystem, subsystem)) {
                    set.add(api.methodName);
                }
            }
            return set;
        }

        @Override
        public boolean subsystemContainsApi(String subsystem, String apiName) {
            for (ApiInfo api : listener.getValues()) {
                if (Objects.equals(api.subsystem, subsystem) && Objects.equals(api.methodName, apiName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getSubsystemNameFromApi(String api) {
            ApiInfo info = getApiInfo(api);
            return info == null ? null : info.subsystem;
        }

        @Override
        public ApiInfo getApiInfo(String apiName) {
            return listener.get(apiName);
        }
    }

    public FileUploadInfoListener createFileUploadInfoListener() {
        return new FileUploadInfoListenerImpl(operator);
    }

    private static class FileUploadInfoListenerImpl implements FileUploadInfoListener {

        private final WatchedResourceListener<ApiInfo> listener;
        private Map<String, FileUploadInfo> cache;

        FileUploadInfoListenerImpl(ZKOperator operator) {
            WatchedResourceEventConsumer eventConsumer = new WatchedResourceEventConsumer() {
                @Override
                public void onChildrenChange(List<String> oldChildren, List<String> newChildren) {
                    cache = null;
                }

                @Override
                public void onChildDataChange(String key, Object oldValue, Object newValue) {
                    cache = null;
                }
            };
            this.listener = new ZKWatchedResourceListener<>(ROOT_PATH, operator, eventConsumer, ApiSummary.class, ApiInfo.class);
        }

        @Override
        public synchronized FileUploadInfo getFileUploadInfo(String folder) {
            if (cache != null) {
                return cache.get(folder);
            }

            Map<String, FileUploadInfo> creating = new HashMap<>();
            for (ApiInfo api : listener.getValues()) {
                if (api != null && api.parameters != null) {
                    for (ApiParamInfo pInfo : api.parameters) {
                        FileUploadInfo fInfo = pInfo.fileUploadInfo;
                        if (fInfo != null) {
                            creating.put(fInfo.folderName, fInfo);
                        }
                    }
                }
            }

            cache = creating;
            return creating.get(folder);
        }
    }
}
