package com.bombadle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BombadleApplication {

	public static void main(String[] args) {
		SpringApplication.run(BombadleApplication.class, args);
	}

}
