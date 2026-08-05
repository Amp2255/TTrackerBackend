package com.ttracker.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

@Getter
@Component
public class GtfsStaticDataCacheService {
    
    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${gtfs.selectedFiles}")
    private String selectedPath;

    private Map<String, String[]> stopsById;
    private Map<String, String[]> tripsById;
    private Map<String, List<String[]>> stopTimesByStopId;
    private List<String> routeShortNames;
    private List<String[]> calendarDateRows;
    
    @PostConstruct
    public void loadFiles() throws IOException{
        System.out.println("Load files started *************** ");
        stopsById = loadCsv("file:" + selectedPath+"/stops.txt", parts -> parts[0].trim());
        tripsById = loadCsv("file:" + selectedPath+"/trips.txt", parts -> parts[2].trim());
        stopTimesByStopId = loadStopTimes();
        routeShortNames = loadRouteShortNames();
        // calendarDateRows = loadRawRows("/calendar_dates.txt");
        System.out.println("Load files done ");
    }

    private Map<String, String[]> loadCsv(String filepath,Function<String [],String> keyExtractor ) {
        Resource resource = resourceLoader.getResource(filepath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            return reader.lines()
                .skip(1)
                .map(line -> line.split(",",-1))
                .filter(parts->parts.length>1)
                .collect(Collectors.toMap(keyExtractor, parts ->parts));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read CSV: " + filepath, ex);
        }
    }

    private Map<String, List<String[]>> loadStopTimes() {
        Resource resource = resourceLoader.getResource("file:" + selectedPath+"/stop_times.txt");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return reader.lines()
                .skip(1)
                .map(line -> line.split(",", -1))
                .filter(parts -> parts.length >= 4)
                .map(parts -> new String[] { parts[3].trim(), parts[0].trim(), parts[1].trim(),parts[2].trim(),parts[4].trim() })
                    // String stopId = parts[3].trim();
                    // String tripId = parts[0].trim();
                    // String arrivalTimeStr = parts[1].trim();
                    //String departureTimeStr = parts[2].trim();
                    //String stopSequence = parts[4].trim();
                 .collect(Collectors.groupingBy(arr -> arr[0]));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read stop_times.txt", ex);
        }
    }

    private List<String[]> loadRawRows(String classpathcalendar_datestxt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private List<String> loadRouteShortNames() {
        Resource resource = resourceLoader.getResource("file:" + selectedPath+"/routes.txt");
        try(BufferedReader reader= new BufferedReader(new InputStreamReader(resource.getInputStream()))){
            return reader.lines().skip(1).collect(Collectors.toList());
        }catch (IOException ex) {
            throw new RuntimeException("Failed to read stop_times.txt", ex);
        }
    }
}
