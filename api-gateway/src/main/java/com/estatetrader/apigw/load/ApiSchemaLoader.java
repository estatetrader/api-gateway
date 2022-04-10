package com.estatetrader.apigw.load;

import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.models.*;
import com.estatetrader.apigw.core.phases.parsing.ApiParser;
import com.estatetrader.rule.definition.ApiDefinitionManager;
import com.estatetrader.rule.definition.ApiInfo;
import com.estatetrader.rule.definition.ApiParamInfo;
import com.estatetrader.util.Lambda;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Service
public class ApiSchemaLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiSchemaLoader.class);

    private final ApiParser apiParser;
    private final ApiSchema apiSchema;
    private final String registryUrl;
    private final int dubboConsumerThreads;
    private final String dubboConsumerThreadPool;
    private final int dubboConsumerQueues;

    public ApiSchemaLoader(
        @Autowired(required = false) ApiDefinitionManager apiDefinitionManager,
        ApiParser apiParser,
        Extensions<ShippedService> shippedServices,
        @Value("${gateway.api-jars.path}") String apiJarsPath,
        @Value("${gateway.application.name}") String applicationName,
        @Value("${dubbo.registry.url}") String registryUrl,
        @Value("${com.estatetrader.apigw.dubboConsumerThreads:8}") int dubboConsumerThreads,
        @Value("${com.estatetrader.apigw.dubboConsumerThreadPool:cached}") String dubboConsumerThreadPool,
        @Value("${com.estatetrader.apigw.dubboConsumerQueues:5000}") int dubboConsumerQueues,
        @Value("${com.estatetrader.dubbo.serialization:hessian2}") String serializationMethod) throws IOException {

        this.apiParser = apiParser;
        this.registryUrl = registryUrl;
        this.dubboConsumerThreads = dubboConsumerThreads;
        this.dubboConsumerThreadPool = dubboConsumerThreadPool;
        this.dubboConsumerQueues = dubboConsumerQueues;

        ApiSchema schema = new ApiSchema();

        apiParser.parseCommonInfo(schema);

        //加载业务接口
        List<ApiMethodInfo> apisInJars = loadApiJars(apiJarsPath, applicationName, serializationMethod);
        apiParser.register(apisInJars, schema);

        // 加载网关内置的API
        for (ShippedService s : shippedServices) {
            List<ApiMethodInfo> shippedApis = apiParser.parseClasses("shipped.jar",
                c -> new SimpleServiceInstance(s), s.getClass());
            apiParser.register(shippedApis, schema);
        }

        apiParser.process(schema);
        apiParser.verify(schema);

        if (apiDefinitionManager != null) {
            // 异步化api同步任务，以提高启动速度，api信息不应阻塞启动过程
            List<ApiInfo> apiInfos = Lambda.map(schema.getApiInfoList(), ApiSchemaLoader::convertToApiInfo);
            CompletableFuture.runAsync(() -> {
                try {
                    apiDefinitionManager.sync(apiInfos);
                    LOGGER.info("api info is synced to zookeeper");
                } catch (Exception e) {
                    LOGGER.error("failed to sync api info to zookeeper", e);
                }
            });
        }

        this.apiSchema = schema;

        for (ShippedService s : shippedServices) {
            if (s instanceof ShippedService.ApiSchemaAware) {
                ((ShippedService.ApiSchemaAware) s).setApiSchema(schema);
            }
        }
    }

    private static ApiInfo convertToApiInfo(ApiMethodInfo info) {
        ApiInfo result = new ApiInfo();
        result.methodName = info.methodName;
        result.groupName = info.groupName;
        result.subsystem = info.subSystem;
        result.authorizing = info.needVerifyAuthorization;

        if (info.parameterInfos != null) {
            result.parameters = new ArrayList<>(info.parameterInfos.length);
            for (ApiParameterInfo pInfo : info.parameterInfos) {
                ApiParamInfo p = new ApiParamInfo();
                p.name = pInfo.name;
                p.type = pInfo.type.getTypeName();
                p.autowired = pInfo.isAutowired;
                p.description = pInfo.description;
                p.fileUploadInfo = pInfo.fileUploadInfo;
                result.parameters.add(p);
            }
        }

        return result;
    }

    @Bean
    public ApiSchema getApiSchema() {
        return apiSchema;
    }

    private ConsumerConfig prepareConsumerConfig() {
        ConsumerConfig consumer = new ConsumerConfig();

        Map<String, String> consumerParams = new HashMap<>();
        consumerParams.put(CommonConstants.THREADS_KEY, String.valueOf(dubboConsumerThreads));
        consumerParams.put(CommonConstants.THREADPOOL_KEY, dubboConsumerThreadPool);
        consumerParams.put(CommonConstants.QUEUES_KEY, String.valueOf(dubboConsumerQueues));
        consumer.setParameters(consumerParams);
        return consumer;
    }

    private List<RegistryConfig> getRegistryConfigs() {
        String[] addressArray = registryUrl.split(" ");
        List<RegistryConfig> registryConfigList = new LinkedList<>();
        for (String zkAddress : addressArray) {
            RegistryConfig registry = new RegistryConfig();
            registry.setAddress(zkAddress);
            registry.setProtocol("dubbo");
            registryConfigList.add(registry);
        }
        return registryConfigList;
    }

    private List<ApiMethodInfo> loadApiJars(String apiJarsPath, String applicationName, String serializationMethod)
        throws IOException {

        File apiJarDirectory = new File(apiJarsPath);
        ApplicationConfig application = new ApplicationConfig();
        application.setName(applicationName);
        application.setParameters(new HashMap<>());
        application.getParameters().put("serialization", serializationMethod);
        ApplicationModel.getConfigManager().setApplication(application);

        // 连接注册中心配置
        List<RegistryConfig> registryConfigList = getRegistryConfigs();
        ConsumerConfig consumer = prepareConsumerConfig();

        // 确定需要加载的jar文件
        if (!apiJarDirectory.exists() || !apiJarDirectory.isDirectory()) {
            throw new IllegalArgumentException("invalid api jar directory: " + apiJarDirectory.getAbsolutePath());
        }

        File[] files = apiJarDirectory.listFiles((f, s) -> s.endsWith(".jar"));
        if (files == null) {
            throw new IllegalArgumentException("invalid api jar folder: " + apiJarDirectory.getAbsolutePath());
        }

        ClassLoader classLoader = getClass().getClassLoader();

        List<String> fileNames = new ArrayList<>(files.length);
        for (File file : files) {
            ClassLoaderUtil.addUrlToClassLoader(classLoader, file.toURI().toURL());
            fileNames.add(file.getPath());
        }

        Map<Class<?>, ServiceInstance> instanceMap = new HashMap<>();
        Function<Class<?>, ServiceInstance> instanceCreator = clazz -> new LazyServiceInstance(
            () -> loadInterface(registryConfigList, consumer, clazz));

        Function<Class<?>, ServiceInstance> instanceGetter =
            clazz -> instanceMap.computeIfAbsent(clazz, instanceCreator);

        return apiParser.parseJars(instanceGetter, fileNames, classLoader);
    }

    private Object loadInterface(List<RegistryConfig> registryConfigList,
                                 ConsumerConfig consumer,
                                 Class<?> clazz) {
        ReferenceConfig<?> reference = createReferenceConfig(registryConfigList, consumer, clazz);
        Object service = reference.get(); // 注意：此代理对象内部封装了所有通讯细节，对象较重，请缓存复用
        if (service == null) {
            throw new IllegalStateException("cannot find dubbo service for " + clazz.getName());
        }

        return service;
    }

    private ReferenceConfig<?> createReferenceConfig(List<RegistryConfig> registryConfigList,
                                                     ConsumerConfig consumer,
                                                     Class<?> clazz) {

        // 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接
        // 引用远程服务
        // 此实例很重，封装了与注册中心的连接以及与提供者的连接，请自行缓存，否则可能造成内存和连接泄漏
        ReferenceConfig<?> reference = new ReferenceConfig<>();
        if (!registryConfigList.isEmpty()) {
            reference.setRegistries(registryConfigList);// 多个注册中心可以用setRegistries()
        }
        reference.setInterface(clazz);
        reference.setCheck(false);
        reference.setAsync(true);
        reference.setVersion("LATEST");

        reference.setConsumer(consumer);
        // 和本地bean一样使用xxxService
        reference.setRetries(0);

        return reference;
    }

    private static class ClassLoaderUtil {
        private static final Method CLASS_LOADER_ADD_URL_METHOD;

        static {
            try {
                CLASS_LOADER_ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                CLASS_LOADER_ADD_URL_METHOD.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        public static void addUrlToClassLoader(URLClassLoader loader, URL url) {
            try {
                CLASS_LOADER_ADD_URL_METHOD.invoke(loader, url);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * convert the loader to URLClassLoader and call addUrlToClassLoader(URLClassLoader loader, URL url)
         */
        public static void addUrlToClassLoader(ClassLoader loader, URL url) {
            addUrlToClassLoader((URLClassLoader) loader, url);
        }
    }
}
