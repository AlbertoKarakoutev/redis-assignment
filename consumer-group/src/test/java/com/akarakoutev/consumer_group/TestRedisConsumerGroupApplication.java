package com.akarakoutev.consumer_group;

import org.springframework.boot.SpringApplication;

public class TestRedisConsumerGroupApplication {

	public static void main(String[] args) {
		SpringApplication.from(RedisConsumerGroupApplication::main).run(args);
	}

}
