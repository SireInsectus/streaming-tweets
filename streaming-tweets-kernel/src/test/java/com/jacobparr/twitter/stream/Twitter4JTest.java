package com.jacobparr.twitter.stream;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tiogasolutions.dev.common.EnvUtils;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

@Test
public class Twitter4JTest {

    private TwitterStreamFactory tsf;

    @BeforeClass
    public void beforeClass() {
        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.setDebugEnabled(true);
        cb.setOAuthConsumerKey(EnvUtils.requireProperty("twitter4j.oauth.consumerKey"));
        cb.setOAuthConsumerSecret(EnvUtils.requireProperty("twitter4j.oauth.consumerSecret"));
        cb.setOAuthAccessToken(EnvUtils.requireProperty("twitter4j.oauth.accessToken"));
        cb.setOAuthAccessTokenSecret(EnvUtils.requireProperty("twitter4j.oauth.accessTokenSecret"));

        Configuration config = cb.build();
        tsf = new TwitterStreamFactory(config);
    }

    public void testSomething() throws InterruptedException {

        StatusListener listener = new StatusListener(){

            @Override
            public void onStatus(Status status) {
                System.out.println(status.getUser().getName() + " : " + status.getText());
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
            }

            @Override
            public void onStallWarning(StallWarning warning) {
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };

        TwitterStream twitterStream = tsf.getInstance();
        twitterStream.addListener(listener);
        twitterStream.sample();

        Thread.sleep(30*1000);
    }
}
