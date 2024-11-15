package com.github.ralgond;

import com.github.ralgond.rls.*;
import com.github.ralgond.rls.db.DBService;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.ibatis.session.SqlSessionFactory;
import java.io.InputStream;

@Configuration
public class AppConfig {

    @Bean
    public NettyServer nettyServer() {
        return new NettyServer();
    }

    @Bean
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

    @Bean
    public RateLimitRedisClient rateLimitRedisClient() {
        return new RateLimitRedisClientSingle();
    }

    @Bean
    public UserSessionRedisClient userSessionRedisClient() {
        return new UserSessionRedisClientSingle();
    }

    @Bean
    public DBService dbService() {
        return new DBService();
    }
}
