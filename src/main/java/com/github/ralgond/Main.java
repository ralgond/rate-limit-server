package com.github.ralgond;

import com.github.ralgond.rls.NettyServer;

public class Main {
    public static void main(String[] args) throws Exception {
        NettyServer server = new NettyServer(8002);
        server.start();
    }
}