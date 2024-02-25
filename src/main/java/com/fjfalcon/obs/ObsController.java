package com.fjfalcon.obs;

import com.fjfalcon.config.ObsProperties;
import com.google.gson.JsonObject;
import io.obswebsocket.community.client.OBSRemoteController;
import io.obswebsocket.community.client.message.request.inputs.SetInputSettingsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ObsController {
    private final Logger logger = LoggerFactory.getLogger(ObsController.class);
    private final ObsProperties obsProperties;
    public ObsController(ObsProperties obsProperties) {
        this.obsProperties = obsProperties;
    }

    public String setUrl(String url) {
        if (url.matches("[0-9]+") && url.length() > 2) {

            OBSRemoteController controller = OBSRemoteController
                    .builder()
                    .port(obsProperties.getPort())
                    .password(obsProperties.getPassword())
                    .host(obsProperties.getHost())
                    .build();

            controller.connect();
            JsonObject object = new JsonObject();
            object.addProperty("url", "https://inetmafia.ru/game/" + url);
            var request = SetInputSettingsRequest.builder().inputName("Inetmafia").inputSettings(object).build();
            controller.sendRequest(request, it -> {
                logger.info("Obs responded with status {}, with data {}", it.getMessageData().getRequestStatus(), it.getMessageData().getResponseData());
            });
            controller.disconnect();

            controller.stop();
            return "Ok";
        } else {
            return "False";
        }
    }

    public String reset() {
            OBSRemoteController controller = OBSRemoteController
                    .builder()
                    .port(obsProperties.getPort())
                    .password(obsProperties.getPassword())
                    .host(obsProperties.getHost())
                    .build();

            controller.connect();
            JsonObject object = new JsonObject();
            object.addProperty("url", "https://inetmafia.ru/");
            var request = SetInputSettingsRequest.builder().inputName("Inetmafia").inputSettings(object).build();
            controller.sendRequest(request, it -> {
            });
            controller.disconnect();

            controller.stop();
            return "Ok";
    }


}
