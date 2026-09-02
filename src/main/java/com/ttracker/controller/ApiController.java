package com.ttracker.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ttracker.dto.ArrivalDto;
import com.ttracker.dto.RouteDto;
import com.ttracker.dto.StopsDto;
import com.ttracker.dto.TimingDto;
import com.ttracker.service.TrackerService;
import com.ttracker.utils.GtfsFilesDownloader;
import com.ttracker.utils.GtfsFilesExtracter;
import com.ttracker.utils.GtfsFilesSelector;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class ApiController {

    private final TrackerService trackerService;
    private final GtfsFilesDownloader downloader;
    private final GtfsFilesExtracter extractor;
    private final GtfsFilesSelector selector;


    @Value("${gtfs.extractedFiles}")
    private String extractedPath;

    @Value("${gtfs.selectedFiles}")
    private String selectedPath;
    public ApiController(TrackerService trackerService, GtfsFilesDownloader downloader,
        GtfsFilesExtracter extractor,GtfsFilesSelector selector) {
        this.trackerService = trackerService;
        this.downloader = downloader;
        this.extractor =  extractor;
        this.selector = selector;
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to TTracker!";
    }

    @GetMapping("/stops")
    public ResponseEntity<List<StopsDto>> getStopByName(@RequestParam String name) {
        if (name.isBlank()) return ResponseEntity.badRequest().body(null);
        List<StopsDto> stops = trackerService.getStopsByName(name);
        if (stops.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(stops);
    }

    // Api to provide the timing of all the lines arriving a 
    // particular station (a stop and its sibling stop).
    @GetMapping("/lines")
    public ResponseEntity<List<ArrivalDto>> getPossibleLines(@RequestParam String stopId) throws IOException {
        if (stopId.isBlank()) return ResponseEntity.badRequest().body(null);
        List<TimingDto> timingDtosList = trackerService.getTimings(stopId);
        if (timingDtosList.isEmpty()) return ResponseEntity.notFound().build();
        List<ArrivalDto> results = new ArrayList<>();
        for (TimingDto timing : timingDtosList) {
            String stopName = trackerService.getStopName(timing.stopId());
            List<RouteDto> routeDtoList = trackerService.getRouteIdFromTripId(timing.tripId());

            for (RouteDto route : routeDtoList) {
                Map<String, String> lineMap = trackerService.getLineFromRoute(route.routeId());
                Map<String, String> lineInfo = lineMap.isEmpty() ? Map.of() : lineMap;
                results.add(new ArrivalDto(timing.stopId(), stopName, timing.tripId(), route.tripHeadsign(), timing.minutesUntil(), route.routeId(), lineInfo));
            }
        }
        if (results.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(results);
    }

    // Api to provide all the lines between two stations 
    @GetMapping("/linesBetweenTwoStops")
    public ResponseEntity<List<ArrivalDto>> getPossibleLinesBetweenStops(@RequestParam String stopIdA,@RequestParam String stopIdB) throws IOException {
        if (stopIdA.isBlank() || stopIdB.isBlank()) return ResponseEntity.badRequest().body(null);
        List<RouteDto> routeDtosList = trackerService.getLinesBetweenStops(stopIdA,stopIdB);
        
        if (routeDtosList.isEmpty()) return ResponseEntity.notFound().build();
        List<ArrivalDto> results = new ArrayList<>();
        for (RouteDto route : routeDtosList) {
                Map<String, String> lineInfo = trackerService.getLineFromRoute(route.routeId());
                results.add(new ArrivalDto(
                    null,                       // stopId (not needed)
                    null,                       // stopName
                    null,                       // tripId
                    route.tripHeadsign(),    // headsign (optional)
                    null,                       // minutesUntil
                    route.routeId(),         // routeId
                    lineInfo                    // includes route_type_label
                ));
            }
            
        return ResponseEntity.ok(results);
    }

    @GetMapping("/routeTime")
    public ResponseEntity<List<ArrivalDto>> getRouteTimings(@RequestParam String stopId, String routeId, String routeTowards) throws IOException {
        if (stopId.isBlank() || routeId.isBlank() ) return ResponseEntity.badRequest().body(null);
        List<TimingDto> timingDtosList = trackerService.getTimings(stopId);
        if (timingDtosList.isEmpty()) return ResponseEntity.notFound().build();
        List<ArrivalDto> results = new ArrayList<>();
        for (TimingDto timing : timingDtosList) {
            String stopName = trackerService.getStopName(timing.stopId());
            List<RouteDto> routeDtoList = trackerService.getRouteIdFromTripId(timing.tripId());

            for (RouteDto route : routeDtoList) {
                if(route.routeId().equals(routeId))
                    continue;
                Map<String, String> lineMap = trackerService.getLineFromRoute(route.routeId());
                Map<String, String> lineInfo = lineMap.isEmpty() ? Map.of() : lineMap;
                results.add(new ArrivalDto(timing.stopId(), stopName, timing.tripId(), route.tripHeadsign(), timing.minutesUntil(), route.routeId(), lineInfo));
            }
        }
        System.out.println("route time api::"+ results.size());
        if (results.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/update-now")
    public void updateNow() {
        System.out.println("-------");
        try{
            Path zip = downloader.download().block();
            Path extracted = Path.of(extractedPath);
            Path selected = Path.of(selectedPath);
            extractor.extract(zip, extracted);
            selector.copyRequiredFiles(extracted, selected);
            System.out.println("GTFS update completed");
        }catch(Exception ex){
            System.out.println(ex);
        }
    }

}
