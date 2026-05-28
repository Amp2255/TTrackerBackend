package com.ttracker.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.reactive.function.client.WebClient;

import com.ttracker.dto.StopsDto;
import com.ttracker.dto.TimingDto;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TrackerServiceTest {

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ScheduledTripsService scheduledTripsService;

    private TrackerService trackerService;

    private final GtfsStaticDataCacheService cacheService = new GtfsStaticDataCacheService();


    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        trackerService = new TrackerService(cacheService);
        inject("resourceLoader", new DefaultResourceLoader());
        inject("webClient", webClient);
        inject("scheduledTripsService", scheduledTripsService);

        // lenient: some tests throw before reaching the webClient call
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        // Mono.empty() → block() returns null, skipping the feed parse block
        lenient().when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());

        lenient().when(scheduledTripsService.getScheduledTimings(anyString())).thenReturn(List.of());
    }

    // --- getStopsByName ---

    @Test
    void getStopsByName_returnsMatchingStops() {
        List<StopsDto> result = trackerService.getStopsByName("central");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StopsDto::getName)
                .containsExactlyInAnyOrder("Central Station", "Central Park");
    }

    @Test
    void getStopsByName_isCaseInsensitive() {
        List<StopsDto> lower = trackerService.getStopsByName("central");
        List<StopsDto> upper = trackerService.getStopsByName("CENTRAL");

        assertThat(lower).usingRecursiveComparison().isEqualTo(upper);
    }

    @Test
    void getStopsByName_throwsOnBlankName() {
        assertThatThrownBy(() -> trackerService.getStopsByName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stop name must not be empty");
    }

    @Test
    void getStopsByName_returnsEmptyListWhenNoMatch() {
        List<StopsDto> result = trackerService.getStopsByName("unknown");

        assertThat(result).isEmpty();
    }

    // --- getStopName ---

    @Test
    void getStopName_returnsNameForKnownStop() {
        assertThat(trackerService.getStopName("S001")).isEqualTo("Central Station");
    }

    @Test
    void getStopName_returnsStopIdWhenNotFound() {
        assertThat(trackerService.getStopName("ZZZNOTEXIST")).isEqualTo("ZZZNOTEXIST");
    }

    // --- getTimings + sibling stop expansion ---

    @Test
    void getTimings_fetchesTimingsForSiblingStops() throws IOException {
        trackerService.getTimings("S001");

        // S001 and S004 share parent_station P001, both should be queried
        verify(scheduledTripsService, atLeastOnce()).getScheduledTimings("S001");
        verify(scheduledTripsService, atLeastOnce()).getScheduledTimings("S004");
    }

    @Test
    void getTimings_mergesRealTimeOverStaticSchedule() throws IOException {
        TimingDto staticEntry = new TimingDto("S001", "TRIP001", 20);
        when(scheduledTripsService.getScheduledTimings("S001")).thenReturn(List.of(staticEntry));

        List<TimingDto> result = trackerService.getTimings("S001");

        // Real-time feed returns empty bytes (no entities), so static entry is kept
        assertThat(result).anyMatch(t -> t.getTripId().equals("TRIP001"));
    }

    @Test
    void getTimings_returnsSortedByArrivalTime() throws IOException {
        when(scheduledTripsService.getScheduledTimings("S001")).thenReturn(List.of(
                new TimingDto("S001", "TRIP002", 30),
                new TimingDto("S001", "TRIP001", 10)
        ));

        List<TimingDto> result = trackerService.getTimings("S001");

        assertThat(result).extracting(TimingDto::getMinutesUntil)
                .isSortedAccordingTo(Long::compare);
    }

    // --- helpers ---

    private void inject(String fieldName, Object value) throws Exception {
        Field field = TrackerService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(trackerService, value);
    }
}
