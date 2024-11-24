package com.github.ralgond.rls;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class UserSessionRedisClientSingle implements UserSessionRedisClient{
    private final JedisPool jedisPool;

    public UserSessionRedisClientSingle(String host, int port, int maxPool) {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxPool);
        jedisPool = new JedisPool(poolConfig, host, port);
    }

    @Override
    public boolean exists(String sessionId) throws Exception {
        Jedis jedis = null;
        try {
            // jedis = jedisPool.getResource();
            jedis = jedisPool.borrowObject();
            boolean ret = jedis.exists(sessionId);
            return ret;
        } finally {
            if (jedis != null) {
                // jedisPool.returnResource(jedis);
                jedisPool.returnObject(jedis);
            }
        }
    }

    @Override
    public void close() {
        jedisPool.close();
    }

}
