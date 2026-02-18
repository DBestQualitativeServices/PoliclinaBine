package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.BlockType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Blocked time slot for a doctor (break, blocked, etc.)")
public class BlockedSlotDto {

    private UUID id;
    private UUID doctorId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private BlockType blockType;
    private String reason;
    private UUID createdByUserId;
    private OffsetDateTime createdAt;
}
