package com.ttracker.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ttracker.dto.ArrivalDto;
import com.ttracker.dto.RouteDto;
import com.ttracker.dto.StopsDto;
import com.ttracker.dto.TimingDto;
import com.ttracker.service.TrackerService;



@CrossOrigin(origins = "*")
@Controller
public class HomeController {

    private final TrackerService trackerService;

    public HomeController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping("/")
    public String homePage() {
        return "home"; // home.html
    }


    // Thymeleaf UI endpoint
    @GetMapping("/stop")
    public String stopView(@RequestParam String stopId, String stopName, Model model) throws IOException {
        
    List<TimingDto> timingDtosList = trackerService.getTimings(stopId);
    List<ArrivalDto> arrivals = new ArrayList<>();

    for (TimingDto timing : timingDtosList) {
        String name = trackerService.getStopName(timing.getStopId());
        List<RouteDto> routeDtoList = trackerService.getRouteIdFromTripId(timing.getTripId());

        for (RouteDto route : routeDtoList) {
            Map<String, String> lineMap = trackerService.getLineFromRoute(route.getRouteId());
            Map<String, String> lineInfo = lineMap.isEmpty() ? Map.of() : lineMap;

            arrivals.add(new ArrivalDto(
                    timing.getStopId(),
                    name,
                    timing.getTripId(),
                    route.getTripHeadsign(),
                    timing.getMinutesUntil(),
                    route.getRouteId(),
                    lineInfo
            ));
        }
    }

    model.addAttribute("stopId", stopId);
    model.addAttribute("stopName",stopName);
    model.addAttribute("arrivals", arrivals);

    return "stop";
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
            String stopName = trackerService.getStopName(timing.getStopId());
            List<RouteDto> routeDtoList = trackerService.getRouteIdFromTripId(timing.getTripId());

            for (RouteDto route : routeDtoList) {
                Map<String, String> lineMap = trackerService.getLineFromRoute(route.getRouteId());
                Map<String, String> lineInfo = lineMap.isEmpty() ? Map.of() : lineMap;
                results.add(new ArrivalDto(timing.getStopId(), stopName, timing.getTripId(), route.getTripHeadsign(), timing.getMinutesUntil(), route.getRouteId(), lineInfo));
            }
        }
        if (results.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(results);
    }

    // Api to provide all the lines between two stations 
    @GetMapping("/linesBetweenTwoStops")
    public String linesBetweenView(@RequestParam String stopIdA,
                                @RequestParam String stopIdB,
                                Model model) throws IOException {

        List<RouteDto> routeDtosList = trackerService.getLinesBetweenStops(stopIdA, stopIdB);

        List<ArrivalDto> results = new ArrayList<>();
        for (RouteDto route : routeDtosList) {
            Map<String, String> lineInfo = trackerService.getLineFromRoute(route.getRouteId());
            results.add(new ArrivalDto(
                    null,
                    null,
                    null,
                    route.getTripHeadsign(),
                    null,
                    route.getRouteId(),
                    lineInfo
            ));
        }

        model.addAttribute("stopAName", trackerService.getStopName(stopIdA));
        model.addAttribute("stopBName", trackerService.getStopName(stopIdB));
        model.addAttribute("lines", results);

        return "lines-between";
    }


    @GetMapping("/routeTime")
    public ResponseEntity<List<ArrivalDto>> getRouteTimings(@RequestParam String stopId, String routeId, String routeTowards) throws IOException {
        if (stopId.isBlank() || routeId.isBlank() ) return ResponseEntity.badRequest().body(null);
        List<TimingDto> timingDtosList = trackerService.getTimings(stopId);
        if (timingDtosList.isEmpty()) return ResponseEntity.notFound().build();
        List<ArrivalDto> results = new ArrayList<>();
        for (TimingDto timing : timingDtosList) {
            String stopName = trackerService.getStopName(timing.getStopId());
            List<RouteDto> routeDtoList = trackerService.getRouteIdFromTripId(timing.getTripId());

            for (RouteDto route : routeDtoList) {
                if(route.getRouteId().equals(routeId))
                    continue;
                Map<String, String> lineMap = trackerService.getLineFromRoute(route.getRouteId());
                Map<String, String> lineInfo = lineMap.isEmpty() ? Map.of() : lineMap;
                results.add(new ArrivalDto(timing.getStopId(), stopName, timing.getTripId(), route.getTripHeadsign(), timing.getMinutesUntil(), route.getRouteId(), lineInfo));
            }
        }
        if (results.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(results);
    }
}
