package com.jacobparr.twit.streamer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server implements Runnable {

    // The selector we'll be monitoring
    private Selector selector;

    // The buffer into which we'll read data when it's available
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    // The pending data for a given socket
    private final Map<SocketChannel,ConcurrentLinkedQueue<String>> pendingData = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<SocketChannel> activeChannels = new CopyOnWriteArrayList<>();

    public Server(InetAddress hostAddress, int port) throws IOException {
        this.selector = this.initSelector(hostAddress, port);
    }

    public void send(String data) {

        synchronized (activeChannels) {
            for (SocketChannel socketChannel: activeChannels) {

                // Indicate we want the interest ops set changed
                socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);

                // And queue the data we want written
                synchronized (pendingData) {
                    ConcurrentLinkedQueue<String> queue = pendingData.computeIfAbsent(socketChannel, k -> new ConcurrentLinkedQueue<>());
                    queue.offer(data);
                }
            }
        }

        // Finally, wake up our selecting thread so it can make the required changes
        selector.wakeup();
    }

    public void run() {
        // noinspection InfiniteLoopStatement
        for (;;) {
            try {
                // Wait for an event one of the registered channels
                selector.select();

                // Iterate over the set of keys for which events are available
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while(it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    // Check what event is available and deal with it
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        // For an accept to be pending the channel must be a server socket channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        activeChannels.add(socketChannel);

        // Register the new SocketChannel with our Selector, indicating
        // we'd like to be notified when there's data waiting to be read
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data
        readBuffer.clear();

        // Attempt to read off the channel
        try {
            int numRead = socketChannel.read(readBuffer);

            if (numRead == -1) {
                // Remote entity shut the socket down cleanly. Do the
                // same from our end and cancel the channel.
                closeSocketChannel(key, socketChannel);
            }

        } catch (IOException e) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            closeSocketChannel(key, socketChannel);
        }

        // yes, sherlock, we are throwing it away.
        // Maybe consider adding a little authentication, or sign in process just so we know.
    }

    private void closeSocketChannel(SelectionKey key, SocketChannel socketChannel) throws IOException {
        key.cancel();
        socketChannel.close();
        activeChannels.remove(socketChannel);

        synchronized (pendingData) {
            pendingData.remove(socketChannel);
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (pendingData) {
            Queue<String> queue = pendingData.get(socketChannel);

            // Write until there's not more data ...
            while (!queue.isEmpty()) {
                String data = queue.poll();

                CharBuffer charBuffer = CharBuffer.wrap(data.toCharArray());
                ByteBuffer buf = Charset.forName("UTF-8").encode(charBuffer);

                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break;
                }
            }

            if (queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested
                // in writing on this socket. Switch back to waiting for
                // data.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private Selector initSelector(InetAddress hostAddress, int port) throws IOException {
        // Create a new selector
        Selector socketSelector = SelectorProvider.provider().openSelector();

        // Create a new non-blocking server socket channel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // Bind the server socket to the specified address and port
        InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
        serverChannel.socket().bind(isa);

        // Register the server socket channel, indicating an interest in accepting new connections
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }
}