package com.github.ralgond.rls;

import com.github.ralgond.rls.db.Rule;

public interface RateLimitRedisClient {
    boolean shouldLimit(String key, Rule rule);
    void close();
}
