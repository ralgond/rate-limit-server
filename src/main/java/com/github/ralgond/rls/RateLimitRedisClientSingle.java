package com.github.ralgond.rls;

import com.github.ralgond.rls.db.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.util.ArrayList;

public class RateLimitRedisClientSingle implements RateLimitRedisClient {

    private static Logger logger  = LoggerFactory.getLogger(RateLimitRedisClientSingle.class);

    private final JedisPool jedisPool;

    public RateLimitRedisClientSingle() {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    @Override
    public boolean shouldLimit(String key, Rule rule) throws Exception {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Object ret = jedis.sendCommand(CustomRedisCommand.CLTHROTTLE,
                    key,
                    String.valueOf(rule.getBurst()),
                    String.valueOf(rule.getTokenCount()),
                    String.valueOf(rule.getTokenTimeUnit()));
            ArrayList<Object> ol = (ArrayList<Object>) ret;
            String limit = ol.get(0).toString();
            if (limit.equals("0")) {
                return false;
            } else {
                return true;
            }
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }
    }

    @Override
    public void close() {
        jedisPool.close();
    }
}
