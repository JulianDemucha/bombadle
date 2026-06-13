package com.bombadle;

import com.bombadle.config.ApplicationConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(ApplicationConfigProperties.class)
@SpringBootApplication
public class BombadleApplication {

    public static void main(String[] args) {
        SpringApplication.run(BombadleApplication.class, args);
    }

}
