package com.github.ralgond.rls;

import redis.clients.jedis.*;

public class RateLimitRedisClientSingle implements RateLimitRedisClient {

    private final JedisPool jedisPool;

    public RateLimitRedisClientSingle() {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    @Override
    public boolean shouldLimit(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Object ret = jedis.sendCommand(CustomRedisCommand.CLTHROTTLE, key, "15", "15", "60");
            // System.out.println(ret);
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

    public static void main(String args[]) {
        var client = new RateLimitRedisClientSingle();
        client.shouldLimit("si_a");
    }
}
