package com.jacobparr.twitter.streaming.server;

import org.tiogasolutions.dev.common.ReflectUtils;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server implements Runnable {

    // The host:port combination to listen on
    private InetAddress hostAddress;
    private int port;

    // The selector we'll be monitoring
    private Selector selector;

    // The buffer into which we'll read data when it's available
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    // A list of PendingChange instances
    private final List<ChangeRequest> pendingChanges = new LinkedList<>();

    // Maps a SocketChannel to a list of ByteBuffer instances
    private final Map<SocketChannel,ConcurrentLinkedQueue<String>> pendingData = new ConcurrentHashMap<>();

    private final List<SocketChannel> activeChannels = new CopyOnWriteArrayList<>();

    public Server(InetAddress hostAddress, int port) throws IOException {
        this.hostAddress = hostAddress;
        this.port = port;
        this.selector = this.initSelector();
    }

    public void send(String data) {

        synchronized (pendingChanges) {
            for (SocketChannel socketChannel: activeChannels) {

                // Indicate we want the interest ops set changed
                // socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);

                pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequestType.CHANGEOPS, SelectionKey.OP_WRITE));

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
                // Process any pending changes
                synchronized (pendingChanges) {
                    for (ChangeRequest change : pendingChanges) {
                        switch (change.getType()) {
                            case CHANGEOPS:
                                SelectionKey key = change.getSocketChannel().keyFor(selector);
                                key.interestOps(change.getOps());
                        }
                    }
                    pendingChanges.clear();
                }

                // Wait for an event one of the registered channels
                selector.select();

                // Iterate over the set of keys for which events are available
                SelectionKey[] keys = ReflectUtils.toArray(SelectionKey.class, selector.selectedKeys());

                for (SelectionKey key : keys) {
                    selector.selectedKeys().remove(key);

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

    private Selector initSelector() throws IOException {
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