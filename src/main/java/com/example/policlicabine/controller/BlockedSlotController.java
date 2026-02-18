package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.BlockedSlotDto;
import com.example.policlicabine.dto.BookingConflictErrorResponse;
import com.example.policlicabine.entity.enums.BlockType;
import com.example.policlicabine.service.BlockedSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blocked-slots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Blocked Slots Management")
public class BlockedSlotController {

    private final BlockedSlotService blockedSlotService;

    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Create a blocked slot for a doctor",
               description = "Blocks a time slot for a doctor. Returns HTTP 409 if the slot overlaps with existing blocked slots or appointments.")
    @ApiResponse(responseCode = "409", description = "Booking conflict - Slot overlaps with existing blocked slots or appointments",
                 content = @Content(schema = @Schema(implementation = BookingConflictErrorResponse.class)))
    public BlockedSlotDto createBlockedSlot(
            @RequestParam UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam BlockType blockType,
            @RequestParam(required = false) String reason,
            @RequestParam UUID createdByUserId
    ) {
        log.info("REST: Creating blocked slot for doctor {} from {} to {} (type: {})",
                doctorId, startTime, endTime, blockType);
        return blockedSlotService.createBlockedSlot(doctorId, startTime, endTime, blockType, reason, createdByUserId);
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "Get blocked slots for a doctor on a specific date",
               description = "Returns all blocked slots for a doctor within the given calendar day (UTC).")
    public List<BlockedSlotDto> getBlockedSlots(
            @RequestParam UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("REST: Getting blocked slots for doctor {} on {}", doctorId, date);
        return blockedSlotService.findByDoctorAndDate(doctorId, date);
    }

    @PutMapping("/{id}")
    @StandardApiResponses
    @Operation(summary = "Update a blocked slot",
               description = "Updates an existing blocked slot. Returns HTTP 409 if the new time range overlaps with other blocked slots or appointments.")
    @ApiResponse(responseCode = "409", description = "Booking conflict - Updated slot overlaps with existing blocked slots or appointments",
                 content = @Content(schema = @Schema(implementation = BookingConflictErrorResponse.class)))
    public BlockedSlotDto updateBlockedSlot(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam BlockType blockType,
            @RequestParam(required = false) String reason
    ) {
        log.info("REST: Updating blocked slot {}", id);
        return blockedSlotService.updateBlockedSlot(id, startTime, endTime, blockType, reason);
    }

    @DeleteMapping("/{id}")
    @StandardApiResponses
    @Operation(summary = "Delete a blocked slot")
    public ResponseEntity<Void> deleteBlockedSlot(@PathVariable UUID id) {
        log.info("REST: Deleting blocked slot {}", id);
        blockedSlotService.deleteBlockedSlot(id);
        return ResponseEntity.noContent().build();
    }
}
