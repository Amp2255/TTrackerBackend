package com.ttracker.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ttracker.dto.ArrivalDto;
import com.ttracker.dto.RouteDto;
import com.ttracker.dto.StopsDto;
import com.ttracker.dto.TimingDto;
import com.ttracker.service.TrackerService;

@CrossOrigin(origins = "*")
@RestController
public class HomeController {

    private final TrackerService trackerService;

    public HomeController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to TTracker!";
    }

    @GetMapping("/stops")
    public ResponseEntity<List<StopsDto>> getStopByName(@RequestParam String name) {
        //System.out.println("Get stop name and id");
        if (name.isBlank()) return ResponseEntity.badRequest().body(null);
        List<StopsDto> stops = trackerService.getStopsByName(name);
        if (stops.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(stops);
    }

    @GetMapping("/lines")
    public ResponseEntity<List<ArrivalDto>> getPossibleLines(@RequestParam String stopId) throws IOException {
        if (stopId.isBlank()) return ResponseEntity.badRequest().body(null);
        List<TimingDto> timingDtosList = trackerService.getTimings(stopId);
        if (timingDtosList.isEmpty()) return ResponseEntity.notFound().build();
        List<ArrivalDto> results = new ArrayList<>();
        for (TimingDto timing : timingDtosList) {
            String stopName = trackerService.getStopName(timing.getStopId());
            List<RouteDto> routeDtoList = trackerService.getRouteIdFromTripId(timing.getTripId());
            for (RouteDto route : routeDtoList) {
                List<Map<String, String>> lineList = trackerService.getLineFromRoute(route.getRouteId());
                Map<String, String> lineInfo = lineList.isEmpty() ? Map.of() : lineList.get(0);
                results.add(new ArrivalDto(timing.getStopId(), stopName, timing.getTripId(), route.getTripHeadsign(), timing.getMinutesUntil(), route.getRouteId(), lineInfo));
            }
        }
        if (results.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(results);
    }


}
