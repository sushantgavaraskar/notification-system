package com.major.delivery.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic deliveryTopic() {
        return TopicBuilder.name("delivery-stream").partitions(6).replicas(1).build();
    }
}
