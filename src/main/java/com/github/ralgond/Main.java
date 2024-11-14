package com.github.ralgond;

import com.github.ralgond.rls.NettyServer;
import com.github.ralgond.rls.db.MyBatisUtil;

public class Main {
    public static void main(String[] args) throws Exception {
        MyBatisUtil.updateAllRules();
        NettyServer server = new NettyServer(8002);
        server.start();
    }
}