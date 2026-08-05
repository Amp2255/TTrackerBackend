package com.ttracker.dto;

public record TripUpdateDto(
        String stopId,
        String tripId,
        long minutesUntil
) {}

