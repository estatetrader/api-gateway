package com.estatetrader.apigw.server;

import com.estatetrader.apigw.core.contracts.GatewayRequest;
import com.estatetrader.apigw.core.contracts.GatewayResponse;
import com.estatetrader.apigw.core.extensions.Extensions;
import com.estatetrader.apigw.request.RequestHandler;
import com.estatetrader.apigw.request.GatewayRequestFilter;
import com.estatetrader.apigw.request.GatewayRequestHandler;
import com.estatetrader.apigw.request.RequestFilter;
import com.estatetrader.apigw.server.model.GatewayNettyRequest;
import com.estatetrader.apigw.server.model.GatewayNettyResponse;
import com.estatetrader.util.Lambda;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static io.netty.buffer.Unpooled.copiedBuffer;

@Component
@ChannelHandler.Sharable
public class GatewayNettyChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayNettyChannelHandler.class);

    private final List<GatewayRequestFilterWrapper> filters;
    private final List<GatewayRequestHandlerWrapper> handlers;

    private final DateFormat DATE_HEADER_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

    public GatewayNettyChannelHandler(
        Extensions<GatewayRequestFilter> filters,
        Extensions<GatewayRequestHandler> handlers) {
        this.filters = Lambda.map(filters, GatewayRequestFilterWrapper::new);
        this.handlers = Lambda.map(handlers, GatewayRequestHandlerWrapper::new);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     * <p>
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.buffer(1024)
            );

            response.headers().set(HttpHeaderNames.DATE, DATE_HEADER_FORMAT.format(new Date()));

            GatewayRequest gatewayRequest = new GatewayNettyRequest(request, ctx);
            GatewayResponse gatewayResponse = new GatewayNettyResponse(response);

            GatewayRequestFilterWrapper.applyAll(filters.iterator(), gatewayRequest, gatewayResponse);
            CompletableFuture<Void> future = processRequest(response, gatewayRequest, gatewayResponse);

            Runnable finish = () -> finishRequest(request, response, ctx);
            if (future == null) {
                finish.run();
            } else {
                future.thenRun(finish);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private CompletableFuture<Void> processRequest(FullHttpResponse response, GatewayRequest gatewayRequest, GatewayResponse gatewayResponse) throws IOException {
        String method = gatewayRequest.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            // 不执行具体逻辑
            return null;
        }

        GatewayRequestHandlerWrapper handler = GatewayRequestHandlerWrapper.firstMatched(handlers, gatewayRequest);

        if (handler == null) {
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            return null;
        }

        return handler.handle(gatewayRequest, gatewayResponse);
    }

    private void finishRequest(FullHttpRequest request, FullHttpResponse response, ChannelHandlerContext ctx) {
        request.release();
        if (!response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().writerIndex());
        }

        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(
                HttpHeaderNames.CONNECTION,
                HttpHeaderValues.KEEP_ALIVE
            );
        }

        ChannelFuture future = ctx.writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(request)) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     * <p>
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.writeAndFlush(new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            copiedBuffer(cause.getMessage().getBytes())
        )).addListener(ChannelFutureListener.CLOSE);
    }

    private static class GatewayRequestFilterWrapper {
        private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

        private final String filterName;
        private final String[] urlPatterns;
        private final GatewayRequestFilter filter;

        GatewayRequestFilterWrapper(GatewayRequestFilter filter) {
            RequestFilter a = filter.getClass().getAnnotation(RequestFilter.class);
            if (a != null) {
                this.filterName = a.filterName();
                this.urlPatterns = a.urlPatterns();
            } else {
                this.filterName = "default";
                this.urlPatterns = new String[0];
            }

            this.filter = filter;
        }

        boolean match(GatewayRequest request) {
            for (String pattern : urlPatterns) {
                if (!PATH_MATCHER.match(pattern, request.getPath())) {
                    return false;
                }
            }
            return true;
        }

        static void applyAll(Iterator<GatewayRequestFilterWrapper> iterator,
                                          GatewayRequest request,
                                          GatewayResponse response) {
            while (iterator.hasNext()) {
                GatewayRequestFilterWrapper wrapper = iterator.next();
                if (wrapper.match(request)) {
                    logger.trace("execute filter {} against request {}", wrapper.filterName, request.getPath());
                    wrapper.filter.filter(request, response,
                        (res, resp) -> applyAll(iterator, res, resp));
                    return;
                }
            }
        }
    }

    private static class GatewayRequestHandlerWrapper {
        private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

        private final String handlerName;
        private final String[] urlPatterns;
        private final String[] methods;
        private final GatewayRequestHandler handler;

        GatewayRequestHandlerWrapper(GatewayRequestHandler handler) {
            RequestHandler a = handler.getClass().getAnnotation(RequestHandler.class);
            if (a != null) {
                this.handlerName = a.handlerName();
                this.urlPatterns = a.urlPatterns();
                this.methods = a.methods();
            } else {
                this.handlerName = "default";
                this.urlPatterns = new String[0];
                this.methods = new String[0];
            }

            this.handler = handler;
        }

        boolean match(GatewayRequest request) {
            return matchPath(request.getPath()) && matchMethod(request.getMethod());
        }

        private boolean matchPath(String path) {
            for (String pattern : urlPatterns) {
                if (PATH_MATCHER.match(pattern, path)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matchMethod(String method) {
            for (String m : methods) {
                if (m.equalsIgnoreCase(method)) {
                    return true;
                }
            }
            return false;
        }

        CompletableFuture<Void> handle(GatewayRequest request, GatewayResponse response) throws IOException {
            logger.trace("execute request {} by handler {}", request.getPath(), handlerName);
            return handler.handle(request, response);
        }

        static GatewayRequestHandlerWrapper firstMatched(List<GatewayRequestHandlerWrapper> list,
                                                         GatewayRequest request) {
            for (GatewayRequestHandlerWrapper w : list) {
                if (w.match(request)) {
                    return w;
                }
            }
            return null;
        }
    }
}
