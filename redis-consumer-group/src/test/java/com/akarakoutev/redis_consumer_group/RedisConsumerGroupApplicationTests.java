package com.akarakoutev.redis_consumer_group;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RedisConsumerGroupApplicationTests {

	@Test
	void contextLoads() {
	}

}
