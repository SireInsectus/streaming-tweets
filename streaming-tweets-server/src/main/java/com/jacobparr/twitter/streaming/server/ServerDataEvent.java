package com.jacobparr.twitter.streaming.server;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

class ServerDataEvent {

    private final Server server;
    private final byte[] data;
    private final Collection<SocketChannel> sockets;

    public ServerDataEvent(Server server, Collection<SocketChannel> sockets, byte[] data) {
        this.server = server;
        this.data = data;

        if (sockets == null) {
            this.sockets = Collections.emptyList();
        } else {
            this.sockets = Collections.unmodifiableList(new ArrayList<>(sockets));
        }
    }

    public Server getServer() {
        return server;
    }

    public byte[] getData() {
        return data;
    }

    public Collection<SocketChannel> getSockets() {
        return sockets;
    }
}