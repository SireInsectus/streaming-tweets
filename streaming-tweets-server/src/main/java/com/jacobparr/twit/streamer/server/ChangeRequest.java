package com.jacobparr.twit.streamer.server;

import java.nio.channels.SocketChannel;

public class ChangeRequest {

    private SocketChannel socketChannel;
    private ChangeRequestType type;
    private int ops;

    public ChangeRequest(SocketChannel socketChannel, ChangeRequestType type, int ops) {
        this.socketChannel = socketChannel;
        this.type = type;
        this.ops = ops;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public ChangeRequestType getType() {
        return type;
    }

    public int getOps() {
        return ops;
    }
}