package com.github.ralgond.rls;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class NettyServer {

    @Autowired
    private RateLimiterHandler rateLimiterHandler;

    public void start(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(3*Runtime.getRuntime().availableProcessors());

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //socketChannel.config().setOption(ChannelOption.SO_SNDBUF, 1024);
                            socketChannel.pipeline().addLast(new ConnectionLimitingHandler());
                            socketChannel.pipeline().addLast(new IdleStateHandler(5,5,10));
                            socketChannel.pipeline().addLast(new HttpServerCodec());
                            socketChannel.pipeline().addLast(new HttpObjectAggregator(1));
                            socketChannel.pipeline().addLast(NettyServer.this.rateLimiterHandler);
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
