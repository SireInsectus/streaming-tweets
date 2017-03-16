package com.jacobparr.twit.streamer;

import com.jacobparr.twit.streamer.server.Server;
import org.tiogasolutions.dev.common.EnvUtils;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

        Server server = new Server(readHost(), readPort());
        new Thread(server).start();

        Configuration config = new ConfigurationBuilder()
            .setDebugEnabled(false)
            .setOAuthConsumerKey(EnvUtils.requireProperty("twitter4j.oauth.consumerKey"))
            .setOAuthConsumerSecret(EnvUtils.requireProperty("twitter4j.oauth.consumerSecret"))
            .setOAuthAccessToken(EnvUtils.requireProperty("twitter4j.oauth.accessToken"))
            .setOAuthAccessTokenSecret(EnvUtils.requireProperty("twitter4j.oauth.accessTokenSecret"))
            .build();

        TwitterStream twitterStream = new TwitterStreamFactory(config).getInstance();
        TwitStatusListener listener = new TwitStatusListener(server);
        twitterStream.addListener(listener);

        twitterStream.sample();
    }

    private int readPort() {
        String propertyName = "twit.streamer.port";
        try {
            String value = EnvUtils.requireProperty(propertyName);
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            String msg = String.format("The specified property, %s, is not a valid integer.", propertyName);
            throw new IllegalArgumentException(msg);
        }
    }

    private InetAddress readHost() {
        String propertyName = "twit.streamer.host";
        try {
            String value = EnvUtils.requireProperty(propertyName);
            return InetAddress.getByName(value);

        } catch (UnknownHostException e) {
            String msg = String.format("The specified property, %s, could not be translated into an instance of %s", propertyName, InetAddress.class.getName());
            throw new IllegalArgumentException(msg);
        }
    }
}
