package com.jacobparr.twitter.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jacobparr.twitter.streaming.server.Server;
import org.tiogasolutions.dev.jackson.TiogaJacksonObjectMapper;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

public class TwitStatusListener implements StatusListener {

    private final Server server;
    private final ObjectMapper objectMapper;

    public TwitStatusListener(Server server) {
        this.server = server;

        objectMapper = new TiogaJacksonObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }

    @Override
    public void onStatus(Status status) {
        try {
            String json = objectMapper.writeValueAsString(status);
            server.send(json.trim().concat("\r\n"));

        } catch (JsonProcessingException e) {
            String json = String.format("{\"error\":\"%s\"}", e.getMessage());
            server.send(json);
        }
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
}
