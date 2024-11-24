package com.github.ralgond.rls;

public interface UserSessionRedisClient {
    boolean exists(String sessionId) throws Exception;
    void close();
}
