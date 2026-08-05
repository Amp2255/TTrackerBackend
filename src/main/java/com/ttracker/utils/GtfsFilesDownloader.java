package com.ttracker.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class GtfsFilesDownloader {

    public GtfsFilesDownloader(org.springframework.web.reactive.function.client.WebClient webClient) {
        this.webClient = webClient;
    }
    
    private final WebClient webClient;


    @Value("${gtfs.downloadpath}")
    private String downloadPath;

    @Value("${gtfs.url}")
    private String gtfsUrl;

    public Mono<Path> download() {

        Path destination = Path.of(downloadPath);

        

        try {
            Files.createDirectories(destination.getParent());
        } catch (IOException e) {
            return Mono.error(e);
        }

        return webClient.get()
                .uri(gtfsUrl)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .transform(data -> DataBufferUtils.write(
                        data,
                        destination,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                ))
                .then(Mono.just(destination));
    }
}
