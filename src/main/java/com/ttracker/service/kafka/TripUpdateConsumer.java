// package com.ttracker.service.kafka;

// import java.util.Collection;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.stereotype.Service;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.ttracker.dto.TripUpdateDto;

// @Service
// public class TripUpdateConsumer {

//     private final ObjectMapper objectMapper = new ObjectMapper();
//     private final Map<String, TripUpdateDto> realTimeCache = new ConcurrentHashMap<>();

//     @KafkaListener(topics = "gtfs.realtime.tripupdates", groupId = "gtfs-consumer")
//     public void consume(String message) {
//         try {
//             TripUpdateDto dto = objectMapper.readValue(message, TripUpdateDto.class);
//             realTimeCache.put(dto.tripId(), dto);
//         } catch (Exception e) {
//             System.out.println("Failed to parse TripUpdate: " + e.getMessage());
//         }
//     }

//     public TripUpdateDto get(String tripId) {
//         return realTimeCache.get(tripId);
//     }

//     public Collection<TripUpdateDto> getAll() {
//         return realTimeCache.values();
//     }
// }
