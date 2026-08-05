// package com.ttracker.service;

// import com.ttracker.dto.TimingDto;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.core.io.DefaultResourceLoader;

// import java.lang.reflect.Field;
// import java.util.List;

// import static org.assertj.core.api.Assertions.assertThat;

// // Note: calendar_dates.txt in test resources uses date 20260407.
// // Tests for "has results" cases will only pass when run on that date.
// // For date-agnostic testing, extract clock injection into ScheduledTripsService.
// class ScheduledTripsServiceTest {

//     private ScheduledTripsService service;

//     @BeforeEach
//     void setUp() throws Exception {
//         service = new ScheduledTripsService();
//         Field field = ScheduledTripsService.class.getDeclaredField("resourceLoader");
//         field.setAccessible(true);
//         field.set(service, new DefaultResourceLoader());
//     }

//     @Test
//     void getScheduledTimings_returnsEmptyForUnknownStop() {
//         List<TimingDto> result = service.getScheduledTimings("UNKNOWN");

//         assertThat(result).isEmpty();
//     }

//     @Test
//     void getScheduledTimings_onlyIncludesTodaysServices() {
//         // TRIP003 belongs to SVC_OLD (date 20200101) and must never appear
//         List<TimingDto> result = service.getScheduledTimings("STOP001");

//         assertThat(result).extracting(TimingDto::getTripId)
//                 .doesNotContain("TRIP003");
//     }

//     @Test
//     void getScheduledTimings_onlyReturnsEntriesForRequestedStop() {
//         List<TimingDto> result = service.getScheduledTimings("STOP002");

//         assertThat(result).allMatch(t -> t.getStopId().equals("STOP002"));
//     }

//     @Test
//     void getScheduledTimings_isSortedByArrivalTime() {
//         List<TimingDto> result = service.getScheduledTimings("STOP001");

//         assertThat(result).extracting(TimingDto::getMinutesUntil)
//                 .isSortedAccordingTo(Long::compare);
//     }

//     @Test
//     void getScheduledTimings_stop001HasTwoValidTrips() {
//         // TRIP001 (SVC001) and TRIP002 (SVC002) are valid today; TRIP003 (SVC_OLD) is not
//         List<TimingDto> result = service.getScheduledTimings("STOP001");

//         assertThat(result).hasSize(2);
//         assertThat(result).extracting(TimingDto::getTripId)
//                 .containsExactly("TRIP001", "TRIP002"); // sorted: 25:00 before 26:00
//     }

//     @Test
//     void getScheduledTimings_stop002HasOneValidTrip() {
//         List<TimingDto> result = service.getScheduledTimings("STOP002");

//         assertThat(result).hasSize(1);
//         assertThat(result.get(0).getTripId()).isEqualTo("TRIP001");
//     }

//     @Test
//     void getScheduledTimings_arrivalsAreInTheFuture() {
//         List<TimingDto> result = service.getScheduledTimings("STOP001");

//         // Arrival times use 25:00+ (beyond midnight), always future relative to LocalTime.now()
//         assertThat(result).allMatch(t -> t.getMinutesUntil() >= 0);
//     }
// }
