package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAvailabilityDto {

    private UUID id;
    private DayOfWeek dayOfWeek;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}
