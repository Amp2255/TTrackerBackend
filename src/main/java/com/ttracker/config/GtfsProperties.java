package com.ttracker.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gtfs")
public record GtfsProperties(
        String url,
        String downloadpath,
        String targetDirectory
) {}

