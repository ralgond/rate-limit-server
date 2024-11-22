package com.github.ralgond.rls;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionLimitingHandler extends ChannelInboundHandlerAdapter {
    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 10000; // 最大连接数

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (connectionCount.incrementAndGet() > MAX_CONNECTIONS) {
            // 超过最大连接数，关闭连接
            ctx.close();
        } else {
            super.channelActive(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionCount.decrementAndGet();
        super.channelInactive(ctx);
    }
}
