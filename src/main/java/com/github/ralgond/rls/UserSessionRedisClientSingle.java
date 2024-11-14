package com.github.ralgond.rls;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class UserSessionRedisClientSingle implements UserSessionRedisClient{
    private final JedisPool jedisPool;

    public UserSessionRedisClientSingle() {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    @Override
    public boolean exists(String sessionId) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            boolean ret = jedis.exists(sessionId);
            return ret;
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

    public static void main(String[] args) {
        var client = new UserSessionRedisClientSingle();
        System.out.println(client.exists("a"));
        System.out.println(client.exists("b"));
    }
}
