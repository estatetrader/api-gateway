package com.estatetrader.apigw.core.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.estatetrader.apigw.core.test.model.TestHttpRequest;
import com.estatetrader.apigw.core.test.model.TestHttpResponse;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.models.ApiMethodInfo;
import com.estatetrader.apigw.core.phases.parsing.ApiParser;
import com.estatetrader.apigw.core.utils.RsaHelper;
import com.estatetrader.core.GatewayException;
import com.estatetrader.responseEntity.StringResp;
import com.estatetrader.util.AESTokenHelper;
import com.estatetrader.algorithm.workflow.WorkflowExecution;
import com.estatetrader.apigw.core.contracts.ServiceInstance;
import com.estatetrader.apigw.core.features.RequestSignatureFeature;
import com.estatetrader.apigw.core.models.ApiSchema;
import com.estatetrader.apigw.core.phases.executing.RequestExecutor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

public abstract class BaseHttpTest {

    private static final AnnotationConfigApplicationContext context;

    static {
        context = new AnnotationConfigApplicationContext();
        for (String name : new String[]{"application.properties"}) {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            Properties properties = new Properties();
            try {
                properties.load(is);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            PropertySource<?> ps = new PropertiesPropertySource(name, properties);
            context.getEnvironment().getPropertySources().addLast(ps);
        }
        context.registerBean("propertyConfigurer",
            PropertySourcesPlaceholderConfigurer.class, PropertySourcesPlaceholderConfigurer::new);

        context.scan("com.estatetrader.apigw.core");
        context.refresh();
    }

    protected String getProperty(String propertyName) {
        return context.getEnvironment().getProperty(propertyName);
    }

    protected <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    protected <T> Extensions<T> getExtensions(Class<T> clazz) {
        return new Extensions.ExtensionsImpl<>(new ArrayList<>(context.getBeansOfType(clazz).values()));
    }

    protected ApiSchema parseApi(Class<?> ... classes) {
        ApiSchema schema = new ApiSchema();
        context.getBean(ApiParser.class).parseClasses(schema, "test.jar", clazz -> null, classes);
        return schema;
    }

    protected List<ApiMethodInfo> parseClasses(Function<Class<?>, ServiceInstance> serviceInstanceGetter, Class<?> ... classes) {
        return context.getBean(ApiParser.class).parseClasses("test.jar", serviceInstanceGetter, classes);
    }

    protected String executeRequest(Map<String, String> params, ApiSchema apiSchema) {
        return executeRequest(params, apiSchema, 1);
    }

    protected String executeRequest(Map<String, String> params, ApiSchema apiSchema, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count < 1");
        }

        RequestExecutor executor = context.getBean(RequestExecutor.class);

        final CountDownLatch lock = new CountDownLatch(count);
        TestHttpResponse lastResponse = null;
        for (int i = 0; i < count; i++) {
            TestHttpRequest request = new TestHttpRequest(params);
            TestHttpResponse response = new TestHttpResponse();
            if (i + 1 == count) {
                lastResponse = response;
            }
            executor.execute(request, response, apiSchema).thenRun(lock::countDown);
        }

        try {
            lock.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        assert lastResponse != null;
        return lastResponse.getBody();
    }

    protected String executeRequest(Map<String, String> params, Class<?> ... apiClasses) {
        ApiSchema apiSchema = parseApi(apiClasses);
        String result = executeRequest(params, apiSchema);
        System.out.println(result);
        return result;
    }

    protected String extractResult(String response) {
        return extractResult(response, StringResp.class, 0).value;
    }

    protected <T> T extractResult(String response, Class<T> clazz) {
        return extractResult(response, clazz, 0);
    }

    protected <T> T extractResult(String response, Class<T> clazz, int index) {
        JSONObject json = JSON.parseObject(response);
        JSONArray contents = json.getJSONArray("content");
        return contents.getObject(index, clazz);
    }

    protected void verifyRequestSignature(String requestUrl, String token) throws GatewayException {
        GatewayRequest request = new TestHttpRequest(retrieveParams(requestUrl));
        ApiContext context = new ApiContext(request, new TestHttpResponse(), new ApiSchema(), new WorkflowExecution[0]);
        if (token != null) {
            context.token = token;
            context.caller = new AESTokenHelper(getAesKey()).parseToken(token);
        }
        getBean(RequestSignatureFeature.RequestVerifierImpl.class).verify(context);
    }

    private Map<String, String> retrieveParams(String url) {
        String query = url.substring(url.indexOf('?') + 1);
        String[] parts = query.split("&");
        Map<String, String> params = new HashMap<>(parts.length);
        for (String part : parts) {
            String key, value;
            int i = part.indexOf('=');
            if (i < 0) {
                key = part;
                value = "";
            } else {
                key = part.substring(0, i);
                try {
                    value = URLDecoder.decode(part.substring(i + 1), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            params.put(key, value);
        }
        return params;
    }

    protected AESTokenHelper getAESTokenHelper() {
        return new AESTokenHelper(getAesKey());
    }

    protected String getAesKey() {
        return getProperty("com.estatetrader.apigw.tokenAes");
    }

    protected RsaHelper getDefaultRSAHelper() {
        return new RsaHelper(getProperty("com.estatetrader.apigw.rsaPublic"), getProperty("com.estatetrader.apigw.rsaPrivate"));
    }
}
