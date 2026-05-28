package com.ttracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimingDto {

    public TimingDto(String stopId2, String tripId2, Long minutesUntil2) {
        this.stopId = stopId2;
        this.tripId = tripId2;
        this.minutesUntil = minutesUntil2;
    }
    private String stopId;
    private String tripId;
    private Long minutesUntil;
    private String arrivalTimeStr;
    private String departureTimeStr;
    private String stopSequence;
}
