package com.ttracker.controller;

import com.ttracker.dto.RouteDto;
import com.ttracker.dto.StopsDto;
import com.ttracker.dto.TimingDto;
import com.ttracker.service.TrackerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackerService trackerService;

    // --- home ---

    @Test
    void home_returnsWelcomeMessage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to TTracker!"));
    }

    // --- /stops ---

    @Test
    void getStopByName_returnsMatchingStops() throws Exception {
        when(trackerService.getStopsByName("central"))
                .thenReturn(List.of(
                        new StopsDto("S001", "Central Station", "addres1"),
                        new StopsDto("S004", "Central Park", "addres14")
                ));

        mockMvc.perform(get("/stops").param("name", "central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("S001"))
                .andExpect(jsonPath("$[0].name").value("Central Station"))
                .andExpect(jsonPath("$[1].id").value("S004"))
                .andExpect(jsonPath("$[1].name").value("Central Park"));
    }

    @Test
    void getStopByName_returnsNotFoundWhenNoMatch() throws Exception {
        when(trackerService.getStopsByName("unknown")).thenReturn(List.of());

        mockMvc.perform(get("/stops").param("name", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStopByName_returnsBadRequestWhenNameIsEmpty() throws Exception {
        mockMvc.perform(get("/stops").param("name", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStopByName_returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(get("/stops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStopByName_returnsInternalServerErrorOnException() throws Exception {
        when(trackerService.getStopsByName("central"))
                .thenThrow(new RuntimeException("Failed to read stops data"));

        mockMvc.perform(get("/stops").param("name", "central"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to read stops data"));
    }

    // --- /lines ---

    @Test
    void getPossibleLines_returnsArrivalData() throws Exception {
        when(trackerService.getTimings("NOX001"))
                .thenReturn(List.of(new TimingDto("NOX001", "TRIP001", 5)));
        when(trackerService.getStopName("NOX001"))
                .thenReturn("Le Bois Habite");
        when(trackerService.getRouteIdFromTripId("TRIP001"))
                .thenReturn(List.of(new RouteDto("TRIP001", "18", "Villeneuve D Ascq")));
        when(trackerService.getLineFromRoute("18"))
                .thenReturn(List.of(Map.of("18", "18")));

        mockMvc.perform(get("/lines").param("stopId", "NOX001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stopId").value("NOX001"))
                .andExpect(jsonPath("$[0].stopName").value("Le Bois Habite"))
                .andExpect(jsonPath("$[0].tripId").value("TRIP001"))
                .andExpect(jsonPath("$[0].destination").value("Villeneuve D Ascq"))
                .andExpect(jsonPath("$[0].arrivalInMinutes").value(5))
                .andExpect(jsonPath("$[0].lineId").value("18"));
    }

    @Test
    void getPossibleLines_returnsNotFoundWhenNoTimings() throws Exception {
        when(trackerService.getTimings("NOX001")).thenReturn(List.of());

        mockMvc.perform(get("/lines").param("stopId", "NOX001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPossibleLines_returnsBadRequestWhenStopIdIsEmpty() throws Exception {
        mockMvc.perform(get("/lines").param("stopId", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPossibleLines_returnsBadRequestWhenStopIdMissing() throws Exception {
        mockMvc.perform(get("/lines"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPossibleLines_returnsMultipleArrivals() throws Exception {
        when(trackerService.getTimings("NOX001"))
                .thenReturn(List.of(
                        new TimingDto("NOX001", "TRIP001", 5),
                        new TimingDto("NOX002", "TRIP002", 12)
                ));
        when(trackerService.getStopName("NOX001")).thenReturn("Le Bois Habite");
        when(trackerService.getStopName("NOX002")).thenReturn("Le Bois Habite");
        when(trackerService.getRouteIdFromTripId("TRIP001"))
                .thenReturn(List.of(new RouteDto("TRIP001", "18", "Villeneuve D Ascq")));
        when(trackerService.getRouteIdFromTripId("TRIP002"))
                .thenReturn(List.of(new RouteDto("TRIP002", "18", "Lille Flandres")));
        when(trackerService.getLineFromRoute("18")).thenReturn(List.of(Map.of("18", "18")));

        mockMvc.perform(get("/lines").param("stopId", "NOX001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].destination").value("Villeneuve D Ascq"))
                .andExpect(jsonPath("$[1].destination").value("Lille Flandres"));
    }
}
