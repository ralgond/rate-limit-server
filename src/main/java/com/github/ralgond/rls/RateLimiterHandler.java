package com.github.ralgond.rls;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.CharsetUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class RateLimiterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterHandler.class);

    private static final ExecutorService executor = Executors.newFixedThreadPool(
            3*Runtime.getRuntime().availableProcessors());
    // private static final ExecutorService executor = Executors.newCachedThreadPool();
    private final RateLimitRedisClient rlRedisClient = new RateLimitRedisClientSingle();
    private final UserSessionRedisClient usRedisClient = new UserSessionRedisClientSingle();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        executor.submit(() -> handleRequest(channelHandlerContext, fullHttpRequest));
    }

    private void handleRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xRealIP = null;
        for (Map.Entry<String,String> entry : request.headers()) {
            if (entry.getKey().equals("X-Real-IP")) {
                xRealIP = entry.getValue();
                break;
            }
        }

        // logger.debug("handleRequest xRealIP={}", xRealIP);

        if (xRealIP == null) {
            sendResponse(ctx, request,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR: X-Real-IP not found.");
            return;
        }

        boolean limitByIp = true;

        String sessionId = null;
        String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);

            for (Cookie cookie : cookies) {
                if (cookie.name().equals("sessionId")) {
                    sessionId = cookie.value();
                    break;
                }
            }
        }

        if (sessionId != null) {
            if (usRedisClient.exists(sessionId)) {
                limitByIp = false; // limit by session id
            }
        }

        String limitKey = null;
        if (limitByIp) {
            limitKey = "ip_" + xRealIP;
        } else {
            limitKey = "si_" + sessionId;
        }

        if (rlRedisClient.shouldLimit(limitKey)) {
            sendResponse(ctx, request,
                    HttpResponseStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS: " + limitKey);
            return;
        } else {
            sendResponse(ctx, request,
                    HttpResponseStatus.OK, "OK: " + limitKey);
            return;
        }
    }

    private void sendResponse(ChannelHandlerContext ctx,
                              FullHttpRequest request, HttpResponseStatus status, String message) {

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void close() {
        rlRedisClient.close();
        usRedisClient.close();
    }
}
