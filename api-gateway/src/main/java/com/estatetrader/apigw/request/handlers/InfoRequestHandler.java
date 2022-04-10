package com.estatetrader.apigw.request.handlers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.load.ApiSchemaLoader;
import com.estatetrader.apigw.request.RequestHandler;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.document.ApiDocument;
import com.estatetrader.util.Lambda;
import com.estatetrader.util.Md5Util;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.request.GatewayRequestHandler;
import com.estatetrader.document.MethodInfo;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

@RequestHandler(handlerName = "info-request", urlPatterns = "/apigw/info.api", methods = "GET")
public class InfoRequestHandler implements GatewayRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoRequestHandler.class);

    private static final SerializerFeature[] SERIALIZER_FEATURES = {
        SerializerFeature.DisableCircularReferenceDetect
    };
    private static final int CACHE_EXPIRE_WINDOW = 5 * 60 * 1000;

    private final boolean infoApiEnabled;
    private final List<String> externalDocUrls;

    private final DocumentFrame internalDocumentFrame;
    private final AtomicReference<DocumentFrame> mergedDocumentFrameCache;
    private final CloseableHttpClient httpClient;

    public InfoRequestHandler(ApiSchemaLoader schemaLoader,
                              @Value("${gateway.info.api.enabled:true}") boolean infoApiEnabled,
                              @Value("${com.estatetrader.openApi.onlyGenerateOpenApiDocument:false}") boolean onlyOpenApi,
                              @Value("${gateway.doc.external-doc-urls:}") String externalDocUrls) {
        this.infoApiEnabled = infoApiEnabled;
        if (externalDocUrls == null || onlyOpenApi) {
            this.externalDocUrls = Collections.emptyList();
        } else {
            this.externalDocUrls = new ArrayList<>();
            for (String part : externalDocUrls.split(" *,+ *")) {
                if (!part.isEmpty()) {
                    this.externalDocUrls.add(part);
                }
            }
        }
        ApiDocument internalDoc = schemaLoader.getApiSchema().document;
        this.internalDocumentFrame = new DocumentFrame(internalDoc, System.currentTimeMillis());
        this.mergedDocumentFrameCache = new AtomicReference<>();
        this.httpClient = HttpClientBuilder.create().build();
    }

    @PostConstruct
    private void init() {
        DocumentFrame frame = generateMergedDocumentFrame();
        mergedDocumentFrameCache.set(frame);
    }

    /**
     * 处理请求
     *
     * @param request  请求
     * @param response 响应
     * @return 表示处理结束的future，返回null表示处理结果同步完成
     */
    @Override
    public CompletableFuture<Void> handle(GatewayRequest request, GatewayResponse response) throws IOException {
        response.setHeader("Cache-Control", "max-age=10");
        response.setContentType("application/json; charset=utf-8");

        if (!infoApiEnabled) {
            OutputStream stream = response.getOutputStream();
            JSON.writeJSONString(stream, Collections.singletonMap("error", "info.api is disabled"));
            return null;
        }

        String merged = request.getParameter("merged");
        DocumentFrame frame;
        if ("merged".equals(merged)) {
            try {
                frame = checkAndGetMergedDocumentFrameCache();
            } catch (Exception e) {
                LOGGER.error("failed to generate document", e);
                OutputStream stream = response.getOutputStream();
                String error = "Failed to generate document: " + e.getMessage();
                JSON.writeJSONString(stream, Collections.singletonMap("error", error));
                return null;
            }
        } else {
            frame = internalDocumentFrame;
        }

        String method = request.getParameter("_mt");

        if (method != null) {
            MethodInfo info = Lambda.find(frame.document.apis, m -> method.equals(m.methodName));
            OutputStream stream = response.getOutputStream();
            JSON.writeJSONString(stream, info, SERIALIZER_FEATURES);
            return null;
        }

        String generic = request.getParameter("generic");
        response.setETag(frame.etag);
        if (frame.etag.equals(request.getIfNoneMatch())) {
            response.sendNotModified();
            return null; // 客户端已拥有最新版本
        }
        response.setContentEncoding("gzip");
        response.getOutputStream().write(frame.serialized);
        return null;
    }

    private DocumentFrame checkAndGetMergedDocumentFrameCache() {
        if (externalDocUrls.isEmpty()) {
            return internalDocumentFrame;
        }
        long now = System.currentTimeMillis();
        DocumentFrame cache = mergedDocumentFrameCache.get();
        if (cache != null && now <= cache.createTime + CACHE_EXPIRE_WINDOW) {
            return cache;
        }
        mergedDocumentFrameCache.set(generateMergedDocumentFrame());
        return mergedDocumentFrameCache.get();
    }

    private DocumentFrame generateMergedDocumentFrame() {
        ApiDocument doc = generateMergedDocument();
        return new DocumentFrame(doc, System.currentTimeMillis());
    }

    private ApiDocument generateMergedDocument() {
        ApiDocument doc = new ApiDocument();
        doc.merge(internalDocumentFrame.document);
        for (String externalDocUrl : externalDocUrls) {
            ApiDocument externalDoc = loadExternalDocument(externalDocUrl);
            doc.merge(externalDoc);
        }
        return doc;
    }

    private ApiDocument loadExternalDocument(String externalDocUrl) {
        HttpGet request = new HttpGet(externalDocUrl);
        request.setHeader("Accept-Encoding", "gzip");
        byte[] bytes;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() >= 400) {
                throw new IllegalApiDefinitionException("the response of the " + externalDocUrl + " is invalid: " + response.getStatusLine());
            }
            bytes = EntityUtils.toByteArray(response.getEntity());
        } catch (IOException e) {
            throw new IllegalApiDefinitionException("could not fetch doc from " + externalDocUrl, e);
        }
        if (bytes == null) {
            throw new IllegalApiDefinitionException("failed to fetch resource from " + externalDocUrl);
        }
        return JSON.parseObject(bytes, ApiDocument.class);
    }

    private static byte[] serializeAndCompress(Object object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            JSON.writeJSONString(gzip, object, SERIALIZER_FEATURES);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private static class DocumentFrame {
        final ApiDocument document;
        final byte[] serialized;
        final String etag;
        final long createTime;

        public DocumentFrame(ApiDocument document, long createTime) {
            this.document = document;
            this.serialized =  serializeAndCompress(document);
            this.etag = Md5Util.computeToBase64(this.serialized);
            this.createTime = createTime;
        }
    }
}
