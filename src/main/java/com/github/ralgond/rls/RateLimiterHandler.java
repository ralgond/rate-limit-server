package com.github.ralgond.rls;

import com.github.ralgond.rls.db.DBService;
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

import com.github.ralgond.rls.db.Rule;
import org.springframework.beans.factory.annotation.Autowired;

@ChannelHandler.Sharable
public class RateLimiterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterHandler.class);

    private final ExecutorService executor;

    @Autowired
    private RateLimitRedisClient rlRedisClient;

    @Autowired
    private UserSessionRedisClient usRedisClient;

    @Autowired
    private DBService dbService;

    public RateLimiterHandler() {
        executor = Executors.newFixedThreadPool(
                3*Runtime.getRuntime().availableProcessors());
    }

    public void close() {
        executor.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        executor.submit(() -> handleRequest(channelHandlerContext, fullHttpRequest));
    }

    private void handleRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xRealIP = null;
        String xRealMethod = null;
        for (Map.Entry<String,String> entry : request.headers()) {
            if (entry.getKey().equals("X-Real-IP")) {
                xRealIP = entry.getValue();
            } else if (entry.getKey().equals("X-Real-Method")) {
                xRealMethod = entry.getValue();
            }
        }

        // logger.debug("handleRequest xRealIP={}", xRealIP);

        if (xRealIP == null) {
            sendResponse(ctx, request,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR: X-Real-IP not found.");
            return;
        }

        if (xRealMethod == null) {
            sendResponse(ctx, request,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR: X-Real-Method not found.");
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

        boolean matched = false;
        Rule matchedRule = null;

        var rules = dbService.getAllRules();

        String limitKey = null;
        if (limitByIp) {
            for (var rule : rules) {
                if (rule.getKeyType().equals("IP") &&
                        rule.getCompiledPathPattern().matcher(request.uri()).matches() &&
                        rule.getMethod().equals(xRealMethod)) {
                    matched = true;
                    matchedRule = rule;
                    break;
                }
            }
            if (!matched) {
                sendResponse(ctx, request,
                        HttpResponseStatus.FORBIDDEN, "FORBIDDEN");
                return;
            }
            limitKey = "ip_" + xRealIP + "_"+ matchedRule.getId();
        } else {
            for (var rule : rules) {
                if (rule.getKeyType().equals("SI") &&
                        rule.getCompiledPathPattern().matcher(request.uri()).matches() &&
                        rule.getMethod().equals(xRealMethod)) {
                    matched = true;
                    matchedRule = rule;
                    break;
                }
            }
            if (!matched) {
                sendResponse(ctx, request,
                        HttpResponseStatus.FORBIDDEN, "FORBIDDEN");
                return;
            }
            limitKey = "si_" + sessionId + "_" + matchedRule.getId();
        }

        try {
            if (rlRedisClient.shouldLimit(limitKey, matchedRule)) {
                sendResponse(ctx, request,
                        HttpResponseStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS: " + limitKey);
            } else {
                sendResponse(ctx, request,
                        HttpResponseStatus.OK, "OK: " + limitKey);
            }
        } catch (Exception e) {
            sendResponse(ctx, request,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR: " + limitKey);
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
}
