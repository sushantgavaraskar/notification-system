package com.major.eventproducer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic rawEventsTopic() {
        return TopicBuilder.name("raw-events").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic enrichedEventsTopic() {
        return TopicBuilder.name("enriched-events").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic actionableNotificationsTopic() {
        return TopicBuilder.name("actionable-notifications").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic deliveryStreamTopic() {
        return TopicBuilder.name("delivery-stream").partitions(6).replicas(1).build();
    }
}
