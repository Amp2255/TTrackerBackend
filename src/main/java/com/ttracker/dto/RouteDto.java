package com.ttracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDto {

    private String tripId;
    private String routeId;
    private String tripHeadsign;
}
