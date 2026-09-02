package com.ttracker.dto;

import java.util.Map;

public record ArrivalDto (

     String stopId,
     String stopName,
     String tripId,
     String towards,
     Long arrivalInMinutes,
     String lineId,
     Map<String, String> lineDetails){

}
