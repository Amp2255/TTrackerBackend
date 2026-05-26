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

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ScheduledTripsService scheduledTripsService;

    public List<StopsDto> getStopsByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stop name must not be empty");
        }
        Resource resource = resourceLoader.getResource("classpath:stops.txt");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            List<StopsDto> stops = reader.lines()
                .filter(line -> line.toLowerCase().contains(name.toLowerCase()))
                .map(line -> {
                    String[] parts = line.split(",",3);
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

            return stops;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read stops data", ex);
        }
    }

public List<TimingDto> getTimings(String stopId) throws IOException {
    //System.out.println("getTimings called with stopId: " + stopId);

    Set<String> allStopIds = getSiblingStopIds(stopId);
    //System.out.println("Fetching timings for stops: " + allStopIds);

    // Step 1: static schedule for all sibling stops as base
    Map<String, TimingDto> mergedByTripId = new LinkedHashMap<>();
    for (String sid : allStopIds) {
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
    Resource resource = resourceLoader.getResource("classpath:stops.txt");
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
        return reader.lines()
            .skip(1)
            .map(line -> line.split(",", 3))
            .filter(parts -> parts.length >= 2 && parts[0].trim().equals(stopId))
            .map(parts -> parts[1].trim())
            .findFirst()
            .orElse(stopId);
    } catch (IOException ex) {
        throw new RuntimeException("Failed to read stops.txt", ex);
    }
}

public List<RouteDto> getRouteIdFromTripId(String tripId) throws IOException {
            //System.out.println("getRouteIdFromTripId called with tripId: " + tripId);
            Resource resource = resourceLoader.getResource("classpath:trips.txt");
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            List<RouteDto> routes = reader.lines()
                .filter(line -> line.split(",").length > 3 && line.split(",")[2].trim().equalsIgnoreCase(tripId))
                .map(line -> {
                    String[] parts = line.split(",");
                    String routeIdString = parts[0].trim();
                    String tripHeadsign = parts[3].trim();
                    //System.out.println("Trip Id: " + tripId + " Route Id: " + routeIdString + " Headsign: " + tripHeadsign);
                    return new RouteDto(tripId, routeIdString, tripHeadsign);
                })
                .collect(Collectors.toList());
            //System.out.println("getRouteIdFromTripId result count for tripId " + tripId + ": " + routes.size());
            return routes;

            } catch (IOException ex) {
                throw new RuntimeException("Failed to read routes data", ex);
            }
}
    public List getLineFromRoute(String routeId){
        //System.out.println("getLineFromRoute called with routeId: " + routeId);
        Resource resource = resourceLoader.getResource("classpath:routes.txt");
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
                    Map<String, String> routeToLine = new HashMap<>();
                    List<Map<String, String>> lineList = new ArrayList<>();
            reader.lines()
                .filter(line -> line.split(",")[0].trim().equalsIgnoreCase(routeId))
                .forEach(line -> {
                    String[] parts = line.split(",",2);
                    String routeIdString = parts[0].trim();
                    //System.out.println("Route Id: " + routeId + " Route Short Name: " + routeIdString);
                    routeToLine.put(routeId, routeIdString);
                    lineList.add(new HashMap<>(routeToLine));
                });
                //System.out.println("getLineFromRoute result count for routeId " + routeId + ": " + lineList.size());
                return lineList;

            } catch (IOException ex) {
                throw new RuntimeException("Failed to read routes data", ex);
            }
    }

}
