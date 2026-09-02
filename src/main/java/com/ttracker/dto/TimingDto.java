package com.ttracker.dto;



public record TimingDto(
    String stopId,
    String tripId,
    Long minutesUntil,
    String arrivalTimeStr,
    String departureTimeStr,
    String stopSequence
) {}
