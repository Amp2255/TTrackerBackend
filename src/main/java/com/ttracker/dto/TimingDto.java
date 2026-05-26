package com.ttracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimingDto {

    private String stopId;
    private String tripId;
    private long minutesUntil;
}
