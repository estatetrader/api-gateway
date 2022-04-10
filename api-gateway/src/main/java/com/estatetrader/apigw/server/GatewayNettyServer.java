package com.estatetrader.apigw.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GatewayNettyServer {

    private final String listenAddress;
    private final int listenPort;
    private final int backlog;
    private final int connectionTimeout;
    private final GatewayChannelInitializer initializer;

    public GatewayNettyServer(@Value("${gateway.listen.address}") String listenAddress,
                              @Value("${gateway.listen.port}") int listenPort,
                              @Value("${gateway.connection-backlog}") int backlog,
                              @Value("${gateway.connection-timeout}") int connectionTimeout,
                              GatewayChannelInitializer initializer) {

        this.listenAddress = listenAddress;
        this.listenPort = listenPort;
        this.backlog = backlog;
        this.connectionTimeout = connectionTimeout;
        this.initializer = initializer;
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Runnable shutdown = () -> {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        };

        Runtime.getRuntime().addShutdownHook(new Thread(shutdown));

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(initializer)
                .option(ChannelOption.SO_BACKLOG, backlog)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout);

            ChannelFuture f = bootstrap.bind(listenAddress, listenPort).sync();
            f.channel().closeFuture().sync();
        } finally {
            shutdown.run();
        }
    }
}
