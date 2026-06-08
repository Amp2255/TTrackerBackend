package com.ttracker.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.ttracker.dto.RouteDto;
import com.ttracker.dto.StopsDto;
import com.ttracker.dto.TimingDto;

@Service
public class TrackerService {

    private final GtfsStaticDataCacheService cacheService;

    public TrackerService(GtfsStaticDataCacheService cacheService) {
        this.cacheService = cacheService;
    }

    
    
    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ScheduledTripsService scheduledTripsService;

    //try catch removed. exception check added. file load on app startup
    public List<StopsDto> getStopsByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stop name must not be empty");
        }
            String normalizedName = name.trim();
            Map<String, String[]> stopsMap = cacheService.getStopsById();
            return stopsMap.values()
                .stream()
                .filter(parts-> 
                    parts.length > 1 &&
                    parts[1] != null && //checks for null pointer exception
                    parts[1].toLowerCase().contains(normalizedName))
                .map(parts -> {
                    String id = parts[0].trim();
                    String stopName = parts[1].trim();
                    String address ="";
                    if(parts.length>2){
                        String [] remParts = parts[2].split(",",2);
                        address = remParts[0].trim();
                    }
                    return new StopsDto(id, stopName, address);
                })
                .collect(Collectors.toList());
        
    }

public List<TimingDto> getTimings(String stopId) throws IOException {
    //System.out.println("getTimings called with stopId: " + stopId);

    Set<String> allStopIds = getSiblingStopIds(stopId);
    //System.out.println("Fetching timings for stops: " + allStopIds);

    // Step 1: static schedule for all sibling stops as base
    Map<String, TimingDto> mergedByTripId = new LinkedHashMap<>();
    for (String sid : allStopIds) {
     //   System.out.println("all stop ids ::::"+ sid);
        for (TimingDto t : scheduledTripsService.getScheduledTimings(sid)) {
            mergedByTripId.put(t.getTripId(), t);
        }
    }

    // Step 2: overlay real-time data where available
    try {
        byte[] responseBytes = webClient.get()
            .uri("https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt")
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
        if (responseBytes != null) {
            InputStream inputStream = new ByteArrayInputStream(responseBytes);
            FeedMessage feed = FeedMessage.parseFrom(inputStream);

            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    String tripId = entity.getTripUpdate().getTrip().getTripId();
                    for (StopTimeUpdate stp : entity.getTripUpdate().getStopTimeUpdateList()) {
                        if (allStopIds.contains(stp.getStopId())) {
                            long eventTime = stp.hasArrival() && stp.getArrival().getTime() != 0
                                ? stp.getArrival().getTime()
                                : stp.getDeparture().getTime();
                            long minutesUntil = (eventTime - (System.currentTimeMillis() / 1000)) / 60;
                            //System.out.println("RT override — Stop: " + stp.getStopId() + " Trip: " + tripId + " in " + minutesUntil + " min");
                            mergedByTripId.put(tripId, new TimingDto(stp.getStopId(), tripId, minutesUntil));
                        }
                    }
                }
            }
        }
    } catch (InvalidProtocolBufferException ex) {
        throw new RuntimeException("Failed to parse feed message", ex);
    }

    List<TimingDto> result = new ArrayList<>(mergedByTripId.values());
    result.sort((a, b) -> Long.compare(a.getMinutesUntil(), b.getMinutesUntil()));
    //System.out.println("getTimings result count for stopId " + stopId + ": " + result.size());
    return result;
}

// Returns the given stopId plus all stops sharing the same parent_station (same physical location)
private Set<String> getSiblingStopIds(String stopId) {
    Resource resource = resourceLoader.getResource("classpath:stops.txt");
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
        List<String[]> allRows = reader.lines()
            .skip(1)
            .map(line -> line.split(",", -1))
            .filter(parts -> parts.length >= 9)
            .collect(Collectors.toList());

        // Find parent_station for the requested stopId
        String parentStation = allRows.stream()
            .filter(parts -> parts[0].trim().equals(stopId))
            .map(parts -> parts[8].trim())
            .findFirst()
            .orElse("");

        if (parentStation.isEmpty()) {
            return Set.of(stopId);
        }

        // Collect all child stops (location_type=0) under the same parent
        Set<String> siblings = allRows.stream()
            .filter(parts -> parts[8].trim().equals(parentStation) && parts[7].trim().equals("0"))
            .map(parts -> parts[0].trim())
            .collect(Collectors.toSet());

        return siblings.isEmpty() ? Set.of(stopId) : siblings;
    } catch (IOException ex) {
        throw new RuntimeException("Failed to read stops.txt", ex);
    }
}

public String getStopName(String stopId) {
    Map<String, String[]> stopMap = cacheService.getStopsById();
    String [] stopsForGivenStopIdString =stopMap.get(stopId);
    if(stopsForGivenStopIdString == null ||stopsForGivenStopIdString.length<2){
        return stopId;
    }
    return stopsForGivenStopIdString[1].trim();
}

public List<RouteDto> getRouteIdFromTripId(String tripId) throws IOException {
            //System.out.println("getRouteIdFromTripId called with tripId: " + tripId);
            Resource resource = resourceLoader.getResource("classpath:trips.txt");
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            return reader.lines()
                .filter(line -> line.split(",").length > 3 && line.split(",")[2].trim().equalsIgnoreCase(tripId))
                .map(line -> {
                    String[] parts = line.split(",");
                    String routeIdString = parts[0].trim();
                    String tripHeadsign = parts[3].trim();
                    //System.out.println("Trip Id: " + tripId + " Route Id: " + routeIdString + " Headsign: " + tripHeadsign);
                    return new RouteDto(routeIdString, tripHeadsign);
                })
                .collect(Collectors.toList());
            //System.out.println("getRouteIdFromTripId result count for tripId " + tripId + ": " + routes.size());

            } catch (IOException ex) {
                throw new RuntimeException("Failed to read routes data", ex);
            }
}
    public Map<String, String> getLineFromRoute(String routeId){
        //System.out.println("getLineFromRoute called with routeId: " + routeId);
        List <String> routeLines =cacheService.getRouteShortNames();
        return routeLines.stream()
        .map(line -> line.split(",", -1)) // split ALL columns
        .filter(parts -> parts[0].trim().equalsIgnoreCase(routeId))
        .findFirst()
        .map(parts -> {
            Map<String, String> map = new HashMap<>();
            map.put("route_id", parts[0].trim());
            map.put("route_short_name", parts[2].trim());
            map.put("route_long_name", parts[3].trim());
            map.put("route_desc", parts[4].trim());
            map.put("route_type", getRouteTypeLabel(parts[5].trim())); //bus/tram/metro etc
            map.put("route_color", parts[7].trim());
            map.put("route_text_color", parts[8].trim());
            return map;
        })
        .orElse(Map.of());
    }

    public List<RouteDto> getLinesBetweenStops(String stopIdA, String stopIdB) {
        Set<String> stopAIds = getSiblingStopIds(stopIdA);
        Set<String> stopBIds = getSiblingStopIds(stopIdB);

        Map<String, List<String[]>> stopTimesByStopId = cacheService.getStopTimesByStopId();
        boolean hasA = stopAIds.stream().anyMatch(stopTimesByStopId::containsKey);
        boolean hasB = stopBIds.stream().anyMatch(stopTimesByStopId::containsKey);

        // System.out.println("Has ANY times for A siblings? " + hasA);
        // System.out.println("Has ANY times for B siblings? " + hasB);

        
        // System.out.println("TrackerService.getlinesBetweenTwoStops() : stopTimes " + stopTimesByStopId.toString().substring(0, 50));
        

        // tripId → stop_sequence
        Map<String, Integer> seqA = stopAIds.stream()
                .filter(stopTimesByStopId::containsKey)
                .flatMap(id -> stopTimesByStopId.get(id).stream())
                .collect(Collectors.toMap(
                    arr -> arr[1], 
                    arr -> Integer.parseInt(arr[4]),
                    Math::min));
       // System.out.println("Seq A map::: "+ seqA.size());            
        Map<String, Integer> seqB = stopBIds.stream()
                .filter(stopTimesByStopId::containsKey)
                .flatMap(id -> stopTimesByStopId.get(id).stream())
                .collect(Collectors.toMap(
                    arr -> arr[1], 
                    arr -> Integer.parseInt(arr[4]), 
                    Math::min));
        //System.out.println("Seq B map::: "+ seqB.size());              
        // System.out.println("Trips at A: " + seqA.keySet());
        // System.out.println("Trips at B: " + seqB.keySet());

        // trips that contain both stops AND A comes before B
        List<String> validTrips = seqA.entrySet().stream()
                .filter(e -> seqB.containsKey(e.getKey()))
                .filter(e -> e.getValue() < seqB.get(e.getKey()))
                .map(Map.Entry::getKey)
                .toList();
       // System.out.println("TrackerService.getlinesBetweenTwoStops() : validTrips " + validTrips.toString());

        // convert tripId → routeId → RouteDto
        return validTrips.stream()
                .flatMap(tripId -> {
                    String[] tripParts = cacheService.getTripsById().get(tripId);
                    if (tripParts == null) return null;
                    String routeId = tripParts[0].trim();
                    String headsign = tripParts[3].trim();
                    return List.of(new RouteDto(routeId, headsign)).stream();
                })
                .distinct()
                .toList();
    }

    public String getRouteTypeLabel(String routeType) {
    return switch (routeType) {
        case "0" -> "Tram";
        case "1" -> "Metro";
        case "2" -> "Rail";
        case "3" -> "Bus";
        case "4" -> "Ferry";
        case "5" -> "Cable Car";
        case "6" -> "Gondola";
        case "7" -> "Funicular";
        default -> "Unknown";
    };
}

}   
