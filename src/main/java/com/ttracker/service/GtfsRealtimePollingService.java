package com.ttracker.service;

import java.io.IOException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.ttracker.dto.TripUpdateDto;
import com.ttracker.service.kafka.TripUpdateProducer;

//Scheduled job → fetch GTFS‑RT → push to Kafka → consumer updates cache → API reads cache
//User → API → read from cache → return instantly

@Service
public class GtfsRealtimePollingService {

    private final TripUpdateProducer producer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    public GtfsRealtimePollingService(TripUpdateProducer producer, WebClient webClient) {
        this.producer = producer;
        this.webClient = webClient; 
    }
    
    @Scheduled(fixedRate = 20000) // every 20 seconds
    public void pollGtfsRealtime() {
        try {
            FeedMessage feed = fetchFromGtfsRtUrl();

            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    TripUpdate tu = entity.getTripUpdate();
                    String tripId = tu.getTrip().getTripId();
                    
                    for (StopTimeUpdate stp : tu.getStopTimeUpdateList()) {
                        long eventTime = stp.hasArrival() && stp.getArrival().getTime() != 0
                                ? stp.getArrival().getTime()
                                : stp.getDeparture().getTime();

                        long minutesUntil = (eventTime - (System.currentTimeMillis() / 1000)) / 60;

                        TripUpdateDto dto = new TripUpdateDto(
                                stp.getStopId(),
                                tripId,
                                minutesUntil
                        );

                        String json = objectMapper.writeValueAsString(dto);
                        producer.sendTripUpdate(tripId, json);
                    }

                                    }
            }

        } catch (Exception e) {
            System.out.println("GTFS polling failed: " + e.getMessage());
        }
    }

    private FeedMessage fetchFromGtfsRtUrl() throws IOException {
        
        byte[] responseBytes = webClient.get()
            .uri("https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt")
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
         if (responseBytes == null) {
        throw new IOException("GTFS-RT feed returned null");}
    

    return FeedMessage.parseFrom(responseBytes);
    
    }
}
