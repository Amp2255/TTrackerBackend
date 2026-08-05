package com.ttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ttracker.config.GtfsProperties;

@EnableScheduling //for gtfs kafka chron job
@SpringBootApplication
@EnableConfigurationProperties(GtfsProperties.class)
@ConfigurationPropertiesScan
public class TTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TTrackerApplication.class, args);
	}

}
