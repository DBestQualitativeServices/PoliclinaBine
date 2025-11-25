package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSessionFilterCriteria {

    private UUID patientId;

    private String patientName;

    private UUID doctorId;

    private String doctorName;

    private OffsetDateTime scheduledAfter;

    private OffsetDateTime scheduledBefore;

    private OffsetDateTime completedAfter;

    private OffsetDateTime completedBefore;

    private SessionStatus status;

    private List<String> consultationNames;
}
