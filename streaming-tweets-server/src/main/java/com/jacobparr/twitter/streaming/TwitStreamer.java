package com.jacobparr.twitter.streaming;

import com.jacobparr.twitter.streaming.server.EchoWorker;
import com.jacobparr.twitter.streaming.server.Server;

public class TwitStreamer {

    public static void main(String... args) throws InterruptedException {
        try {
            new TwitStreamer().start();

        } catch (Exception e) {
            System.out.flush();
            System.err.flush();
            e.printStackTrace();

            Thread.sleep(1000);
            System.exit(0);
        }
    }

    public TwitStreamer() {
    }

    private void start() throws Exception {
        EchoWorker worker = new EchoWorker();
        new Thread(worker).start();

        new Thread(new Server(null, 9090, worker)).start();

    }
}
