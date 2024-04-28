package com.fjfalcon.obs;

import com.fjfalcon.config.ObsProperties;
import com.google.gson.JsonObject;
import io.obswebsocket.community.client.OBSRemoteController;
import io.obswebsocket.community.client.message.request.inputs.SetInputSettingsRequest;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ObsController {
    private final Logger logger = LoggerFactory.getLogger(ObsController.class);
    private final OBSRemoteController controller;

    private boolean isConnected;

    public ObsController(ObsProperties obsProperties) {
        this.controller = OBSRemoteController
                .builder()
                .port(obsProperties.getPort())
                .password(obsProperties.getPassword())
                .host(obsProperties.getHost())
                .lifecycle()
                .onClose((ignored) -> isConnected = false)
                .and()
                .build();

        connect();
    }

    private void connect() {
        controller.connect();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        isConnected = true;
    }

    public String setUrl(String url) {
        try {
            if (url.matches("[0-9]+") && url.length() > 2) {
                if (!isConnected) {
                    connect();
                }
                JsonObject object = new JsonObject();
                object.addProperty("url", "https://inetmafia.ru/game/" + url);
                var request = SetInputSettingsRequest.builder().inputName("Inetmafia").inputSettings(object).build();
                controller.sendRequest(request, it -> {
                    logger.info("Obs responded with status {}, with data {}",
                            it.getMessageData().getRequestStatus(),
                            it.getMessageData().getResponseData());
                });
                return "Ok";
            } else {
                return "Error: wrong command: Usage is: /set 99999";
            }
        } catch (Exception e) {
            logger.error("Error during reset, trying to reconnect", e);
            controller.connect();
            return setUrl(url);
        }

    }

    public String reset() {
        try {
            if (!isConnected) {
                connect();
            }
            JsonObject object = new JsonObject();
            object.addProperty("url", "https://inetmafia.ru/");
            var request = SetInputSettingsRequest.builder().inputName("Inetmafia").inputSettings(object).build();
            controller.sendRequest(request, it -> logger.info("Obs responded with status {}, with data {}",
                    it.getMessageData().getRequestStatus(),
                    it.getMessageData().getResponseData()));

            return "Ok";
        } catch (Exception e) {
            logger.error("Error during set command, trying to reconnect", e);
            controller.connect();
            return reset();
        }
    }

    public String startStreaming() {
        var response = controller.startStream(1000L);
        logger.info("Obs responded with status {}, with data {}", response.getMessageData().getRequestStatus(), response.getMessageData().getResponseData());
        return "ok";
    }

    public String stopStreaming() {
        var response = controller.stopStream(1000L);
        logger.info("Obs responded with status {}, with data {}", response.getMessageData().getRequestStatus(), response.getMessageData().getResponseData());
        return "ok";
    }

    @PreDestroy
    public void close() {
        controller.disconnect();
        controller.stop();
    }

    public void setStreamKey(String key) {
        logger.info("Setting key to {}", key);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("key", key);
        controller.setStreamServiceSettings("rtmp_common", jsonObject, response ->
                logger.info("Obs responded with status {}, with data {}", response.getMessageData().getRequestStatus(), response.getMessageData().getResponseData()));
    }

    public boolean isStreamEnabled() {
        var status = controller.getStreamStatus(1000L);
        logger.info("Obs responded with status {}, with data {}", status.getMessageData().getRequestStatus(), status.getMessageData().getResponseData());
        return status.getMessageData().getResponseData().getOutputActive();
    }

    public String getUrl() {
        var settings = controller.getInputSettings("Inetmafia", 1000L);
        return settings.getInputSettings().get("url").getAsString();
    }
}
