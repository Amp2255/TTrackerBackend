package com.ttracker.service.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TripUpdateProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    public TripUpdateProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTripUpdate(String tripId, String json) {
        kafkaTemplate.send("gtfs.realtime.tripupdates", tripId, json);
    }
}
