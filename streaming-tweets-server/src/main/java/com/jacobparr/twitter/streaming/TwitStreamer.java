package com.jacobparr.twitter.streaming;

import com.jacobparr.twitter.streaming.server.Server;
import org.tiogasolutions.dev.common.EnvUtils;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

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

    private final Configuration config;

    public TwitStreamer() {
        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.setDebugEnabled(false);
        cb.setOAuthConsumerKey(EnvUtils.requireProperty("twitter4j.oauth.consumerKey"));
        cb.setOAuthConsumerSecret(EnvUtils.requireProperty("twitter4j.oauth.consumerSecret"));
        cb.setOAuthAccessToken(EnvUtils.requireProperty("twitter4j.oauth.accessToken"));
        cb.setOAuthAccessTokenSecret(EnvUtils.requireProperty("twitter4j.oauth.accessTokenSecret"));

        config = cb.build();
    }

    private void start() throws Exception {

        Server server = new Server(null, 9090);
        new Thread(server).start();

        TwitterStreamFactory tsf = new TwitterStreamFactory(config);
        TwitterStream twitterStream = tsf.getInstance();

        TwitStatusListener listener = new TwitStatusListener(server);
        twitterStream.addListener(listener);

        twitterStream.sample();
    }
}
