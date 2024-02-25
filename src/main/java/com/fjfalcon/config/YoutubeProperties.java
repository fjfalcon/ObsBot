package com.fjfalcon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("youtube")
public class YoutubeProperties {
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
