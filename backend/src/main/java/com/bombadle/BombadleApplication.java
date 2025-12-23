package com.bombadle;

import com.bombadle.config.ApplicationConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(ApplicationConfigProperties.class)
@SpringBootApplication
public class BombadleApplication {

    public static void main(String[] args) {
        SpringApplication.run(BombadleApplication.class, args);
    }

}
