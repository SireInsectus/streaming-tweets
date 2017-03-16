package com.jacobparr.twitter.streaming.server;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class EchoWorker implements Runnable {

    private final List<ServerDataEvent> queue = new LinkedList<>();

    public void processData(Server server, Collection<SocketChannel> sockets, byte[] data, int count) {
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        synchronized(queue) {
            queue.add(new ServerDataEvent(server, sockets, dataCopy));
            queue.notify();
        }
    }

    public void run() {
        ServerDataEvent dataEvent;

        for(;;) {
            // Wait for data to become available
            synchronized(queue) {
                while(queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                    }
                }
                dataEvent = queue.remove(0);
            }

            // Return to sender
            dataEvent.getServer().send(dataEvent.getSockets(), dataEvent.getData());
        }
    }
}