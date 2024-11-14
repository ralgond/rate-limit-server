package com.github.ralgond.rls.db;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MyBatisUtil {
    private static SqlSessionFactory sqlSessionFactory;
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static List<Rule> allRules;
    private static ReadWriteLock rwLock = new ReentrantReadWriteLock();


    static {
        try {
            InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateAllRules();

        executor.scheduleAtFixedRate(()->{
            updateAllRules();
            // System.out.println(getAllRules());
        }, 0, 5, TimeUnit.SECONDS);
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public static SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }

    public static List<Rule> getAllRules() {
        try {
            rwLock.readLock().lock();
            return allRules;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static void updateAllRules() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            RuleMapper mapper = session.getMapper(RuleMapper.class);
            var tmpAllRules = mapper.getAllRules();

            for (var rule : tmpAllRules) {
                rule.updateCompiledPathPattern();
            }

            try{
                rwLock.writeLock().lock();
                allRules = tmpAllRules;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    public static void main(String[] args) {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }
        }
    }
}
