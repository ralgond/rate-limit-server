package com.github.ralgond.rls.db;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class DBService {
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    private final ScheduledExecutorService executor;
    private List<Rule> allRules;
    private final ReadWriteLock rwLock;

    public DBService() {
        executor = Executors.newSingleThreadScheduledExecutor();
        rwLock = new ReentrantReadWriteLock();
    }

    public void start() {
        updateAllRules();

        executor.scheduleAtFixedRate(()->{
            updateAllRules();
            // System.out.println(getAllRules());
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.close();
    }

    public SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }

    public List<Rule> getAllRules() {
        try {
            rwLock.readLock().lock();
            return allRules;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void updateAllRules() {
        try (SqlSession session = openSession()) {
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
}
