package com.github.ralgond.rls;

public interface RateLimitRedisClient {
    boolean shouldLimit(String key);
    void close();
}
