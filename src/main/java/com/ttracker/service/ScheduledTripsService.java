package com.ttracker.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.ttracker.dto.TimingDto;

@Service
public class ScheduledTripsService {

    @Value("${gtfs.selectedFiles}")
    private String selectedPath;

    private final ResourceLoader resourceLoader;

    ScheduledTripsService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<TimingDto> getScheduledTimings(String stopId) {
        Set<String> serviceIds = getValidServiceIds();
        Set<String> tripIds = getValidTripIds(serviceIds);
        return getTimingsForStop(stopId, tripIds);
    }

    private Set<String> getValidServiceIds() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Resource resource = resourceLoader.getResource("file:"+ selectedPath +"/calendar_dates.txt");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return reader.lines()
                .skip(1)
                .map(line -> line.split(","))
                .filter(parts -> parts.length >= 3
                    && parts[1].trim().equals(today)
                    && parts[2].trim().equals("1"))
                .map(parts -> parts[0].trim())
                .collect(Collectors.toSet());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read calendar_dates.txt", ex);
        }
    }

    private Set<String> getValidTripIds(Set<String> serviceIds) {
        Resource resource = resourceLoader.getResource("file:" + selectedPath+"/trips.txt");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return reader.lines()
                .skip(1)
                .map(line -> line.split(","))
                .filter(parts -> parts.length >= 3 && serviceIds.contains(parts[1].trim()))
                .map(parts -> parts[2].trim())
                .collect(Collectors.toSet());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read trips.txt", ex);
        }
    }

    private List<TimingDto> getTimingsForStop(String stopId, Set<String> validTripIds) {
        Resource resource = resourceLoader.getResource("file:" + selectedPath+"/stop_times.txt");
        LocalTime now = LocalTime.now();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return reader.lines()
                .skip(1)
                .map(line -> line.split(","))
                .filter(parts -> parts.length >= 4
                    && parts[3].trim().equals(stopId)
                    && validTripIds.contains(parts[0].trim()))
                .map(parts -> {
                    String tripId = parts[0].trim();
                    String arrivalTimeStr = parts[1].trim();
                    String departureTimeStr = parts[2].trim();
                    String stopSequence= parts[4].trim();
                    long minutesUntil = parseMinutesUntil(arrivalTimeStr, now);
                    return new TimingDto(stopId, tripId, minutesUntil, arrivalTimeStr, departureTimeStr, stopSequence);
                })
                .filter(t -> t.minutesUntil() >= 0)
                .sorted((a, b) -> Long.compare(a.minutesUntil(), b.minutesUntil()))
                .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read stop_times.txt", ex);
        }
    }

    // GTFS times can exceed 24:00:00 for overnight trips
    private long parseMinutesUntil(String timeStr, LocalTime now) {
        String[] parts = timeStr.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        long totalMinutes = hours * 60L + minutes;
        long nowMinutes = now.getHour() * 60L + now.getMinute();
        return totalMinutes - nowMinutes;
    }
}
