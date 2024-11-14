package com.github.ralgond.rls;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

public class RateLimitRedisClientCluster implements RateLimitRedisClient{
    private final JedisCluster jedisCluster;

    public RateLimitRedisClientCluster() {
        Set<HostAndPort> clusterNodes = new HashSet<>();
        clusterNodes.add(new HostAndPort("localhost", 6379));

        int connectionTimeout = 2000;
        int soTimeout = 2000;
        int maxAttempts = 5;

        var poolConfig = new GenericObjectPoolConfig<Connection>();
        poolConfig.setMaxTotal(100);
        jedisCluster = new JedisCluster(clusterNodes, connectionTimeout, soTimeout, maxAttempts, poolConfig);
    }

    public void close() {
        jedisCluster.close();
    }

    public boolean shouldLimit(String key) {
        Object ret = jedisCluster.sendCommand(Protocol.Command.valueOf("CL.THROTTLE"), key, "15", "15", "60");
        System.out.println(ret);

        return false;
    }

    public static void main(String args[]) {
        var client = new RateLimitRedisClientCluster();
        client.shouldLimit("si_a");
        client.close();
    }
}
