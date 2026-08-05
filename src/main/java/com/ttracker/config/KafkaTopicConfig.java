package com.ttracker.config;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;


@Configuration
public class KafkaTopicConfig {

    // This class tells the application:" When the app starts, make sure this Kafka topic exists.
    // If it doesn’t, create it. If it already exists, do nothing."
    @Bean
    public NewTopic tripUpdatesTopic() {
        return TopicBuilder.name("gtfs.realtime.tripupdates")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

