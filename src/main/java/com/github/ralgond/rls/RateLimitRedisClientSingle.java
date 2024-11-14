package com.github.ralgond.rls;

import com.github.ralgond.rls.db.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

public class RateLimitRedisClientSingle implements RateLimitRedisClient {

    private static Logger logger  = LoggerFactory.getLogger(RateLimitRedisClientSingle.class);

    private final JedisPool jedisPool;

    public RateLimitRedisClientSingle() {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    @Override
    public boolean shouldLimit(String key, Rule rule) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Object ret = jedis.sendCommand(CustomRedisCommand.CLTHROTTLE,
                    key,
                    String.valueOf(rule.getBurst()),
                    String.valueOf(rule.getTokenCount()),
                    String.valueOf(rule.getTokenTimeUnit()));
            logger.debug(ret.getClass().getCanonicalName());
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }

        return false;
    }

    @Override
    public void close() {
        jedisPool.close();
    }
}
