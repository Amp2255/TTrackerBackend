package com.ttracker.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArrivalDto {

    private String stopId;
    private String stopName;
    private String tripId;
    private String destination;
    private long arrivalInMinutes;
    private String lineId;
    private Map<String, String> lineDetails;
}
