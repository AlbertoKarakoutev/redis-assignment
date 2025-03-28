package com.akarakoutev.redis_consumer_group;

import org.springframework.boot.SpringApplication;

public class TestRedisConsumerGroupApplication {

	public static void main(String[] args) {
		SpringApplication.from(RedisConsumerGroupApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
