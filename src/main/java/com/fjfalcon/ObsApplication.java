package com.fjfalcon;

import com.fjfalcon.config.BotProperties;
import com.fjfalcon.config.ObsProperties;
import com.fjfalcon.config.YoutubeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BotProperties.class, ObsProperties.class, YoutubeProperties.class})
public class ObsApplication {


    public static void main(String[] args) {
        SpringApplication.run(ObsApplication.class);


    }
}