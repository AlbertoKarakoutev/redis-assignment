package com.akarakoutev.consumer_group.consumption;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
public class ConsumerConfig {

    @Bean
    public Set<AbstractConsumer<?>> consumerGroup(
            AnnotationConfigApplicationContext annotationConfigApplicationContext,
            @Value("${consumer.group.size}") int consumerGroupSize
    ) {
        AutowireCapableBeanFactory beanFactory = annotationConfigApplicationContext.getBeanFactory();
        return IntStream.range(0, consumerGroupSize)
                .mapToObj(index -> beanFactory.getBean(MessageIdJsonConsumer.class))
                .collect(Collectors.toSet());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
