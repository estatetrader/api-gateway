package com.estatetrader.apigw.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GatewayChannelInitializer extends ChannelInitializer<Channel> {

    private final GatewayNettyChannelHandler requestHandler;
    private final int maxContentSize;

    public GatewayChannelInitializer(
        GatewayNettyChannelHandler requestHandler,
        @Value("${gateway.max-content-size}") int maxContentSize) {

        this.requestHandler = requestHandler;
        this.maxContentSize = maxContentSize;
    }

    /**
     * This method will be called once the {@link Channel} was registered. After the method returns this instance
     * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
     *
     * @param ch the {@link Channel} which was registered.
     * @throws Exception is thrown if an error occurs. In that case it will be handled by
     *                   {@link #exceptionCaught(ChannelHandlerContext, Throwable)} which will by default close
     *                   the {@link Channel}.
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast("codec", new HttpServerCodec());
        ch.pipeline().addLast("decompressor", new HttpContentDecompressor());
        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(maxContentSize));
        ch.pipeline().addLast("request", requestHandler);
    }
}
