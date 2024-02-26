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

    @PreDestroy
    public void close() {
        controller.disconnect();
        controller.stop();
    }

}
