package com.github.ralgond;

import com.github.ralgond.rls.NettyServer;
import com.github.ralgond.rls.db.DBService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) throws Exception {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        DBService dbService = context.getBean(DBService.class);
        dbService.start();

        NettyServer server = context.getBean(NettyServer.class);
        server.start(8002);
    }
}