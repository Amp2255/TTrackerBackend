package com.ttracker.utils;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class GtfsDownloadScheduler {
    private final GtfsFilesDownloader downloader;
    private final GtfsFilesExtracter extractor;
    private final GtfsFilesSelector selector;


    @Value("${gtfs.extractedFiles}")
    private String extractedPath;

    @Value("${gtfs.selectedFiles}")
    private String selectedPath;

    public GtfsDownloadScheduler(GtfsFilesDownloader downloader, GtfsFilesExtracter extractor, GtfsFilesSelector selector) {
        this.downloader = downloader;
        this.extractor = extractor;
        this.selector =selector;
    }

    @Scheduled(cron = "0 0 3 * * *")
public void updateGtfs() throws Exception {
    Path zip = downloader.download().block();
    Path extracted = Path.of(extractedPath);
    Path selected = Path.of(selectedPath);
    extractor.extract(zip, extracted);
    selector.copyRequiredFiles(extracted, selected);
    System.out.println("GTFS update completed");
}
}
