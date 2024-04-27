package com.fjfalcon;

import com.fjfalcon.config.BotProperties;
import com.fjfalcon.config.ObsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({BotProperties.class, ObsProperties.class})
@EnableScheduling
public class ObsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ObsApplication.class);
    }
}