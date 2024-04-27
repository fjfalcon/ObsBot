package com.fjfalcon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bot")
public class BotProperties {
    private String name;
    private String password;
    private Long myChatId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getMyChatId() {
        return myChatId;
    }

    public void setMyChatId(Long myChatId) {
        this.myChatId = myChatId;
    }
}
