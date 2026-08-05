package com.ttracker.utils;

import java.io.IOException;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;

@Component
public class GtfsFilesSelector {
    
    private static final List<String> REQUIRED_FILES = List.of(
            "stop_times.txt",
            "calendar_dates.txt",
            "routes.txt",
            "stops.txt",
            "trips.txt"
    );

    public void copyRequiredFiles(Path extractedDir, Path selectedFilesDir) 
        throws IOException{
        Files.createDirectories(selectedFilesDir);

        for (String fileName : REQUIRED_FILES) {

            Path source = extractedDir.resolve(fileName);
            Path target = selectedFilesDir.resolve(fileName);

            if (Files.exists(source)) {
                Files.copy(
                        source,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } else {
                throw new IOException("Missing GTFS file: " + fileName);
            }
        }
        
    }

}
