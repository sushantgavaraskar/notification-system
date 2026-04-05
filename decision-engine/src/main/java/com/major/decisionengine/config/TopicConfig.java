package com.major.decisionengine.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic actionableTopic() {
        return TopicBuilder.name("actionable-notifications").partitions(6).replicas(1).build();
    }
}
