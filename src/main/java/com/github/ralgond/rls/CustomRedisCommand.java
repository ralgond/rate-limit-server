package com.github.ralgond.rls;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public enum CustomRedisCommand implements ProtocolCommand {
    CLTHROTTLE("CL.THROTTLE");

    private final byte[] raw;

    CustomRedisCommand(String command) {
        this.raw = SafeEncoder.encode(command);
    }
    public byte[] getRaw() {
        return this.raw;
    }
}
