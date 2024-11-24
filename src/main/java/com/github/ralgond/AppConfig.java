package com.github.ralgond;

import com.github.ralgond.rls.*;
import com.github.ralgond.rls.db.DBService;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import org.apache.ibatis.session.SqlSessionFactory;
import java.io.InputStream;

@Configuration
@PropertySource("classpath:external-config.properties")
public class AppConfig {

    @Value("${NettyServer.port}")
    int NettyServerPort;

    @Bean
    public NettyServer nettyServer() {
        return new NettyServer(NettyServerPort);
    }

    @Bean(destroyMethod = "close")
    public RateLimiterHandler rateLimiterHandler() {
        return new RateLimiterHandler();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() {
        try {
            InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            return sqlSessionFactory;
        } catch (Exception e) {
            return null;
        }
    }

    @Value("${RateLimitRedisClientSingle.host}")
    String RateLimitRedisClientSingleHost;

    @Value("${RateLimitRedisClientSingle.port}")
    int RateLimitRedisClientSinglePort;

    @Value("${RateLimitRedisClientSingle.maxPool}")
    int RateLimitRedisClientSingleMaxPool;

    @Bean(destroyMethod = "close")
    public RateLimitRedisClient rateLimitRedisClient() {
        return new RateLimitRedisClientSingle(RateLimitRedisClientSingleHost,
                RateLimitRedisClientSinglePort,
                RateLimitRedisClientSingleMaxPool);
    }

    @Value("${UserSessionRedisClientSingle.host}")
    String UserSessionRedisClientSingleHost;

    @Value("${UserSessionRedisClientSingle.port}")
    int UserSessionRedisClientSinglePort;

    @Value("${UserSessionRedisClientSingle.maxPool}")
    int UserSessionRedisClientSingleMaxPool;

    @Bean(destroyMethod = "close")
    public UserSessionRedisClient userSessionRedisClient() {
        return new UserSessionRedisClientSingle(UserSessionRedisClientSingleHost,
                UserSessionRedisClientSinglePort,
                UserSessionRedisClientSingleMaxPool);
    }

    @Bean(destroyMethod = "stop")
    public DBService dbService() {
        return new DBService();
    }
}
