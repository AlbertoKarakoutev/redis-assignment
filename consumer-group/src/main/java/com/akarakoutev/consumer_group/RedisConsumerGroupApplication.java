package com.akarakoutev.consumer_group;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RedisConsumerGroupApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisConsumerGroupApplication.class, args);
	}

}
