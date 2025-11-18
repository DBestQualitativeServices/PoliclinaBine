package com.example.policlicabine.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event published when a file is soft-deleted.
 *
 * <p>This event is published after the file's isDeleted flag is set and
 * deletion metadata is recorded. Event listeners can use this for:
 * <ul>
 *   <li>Sending deletion confirmation notifications</li>
 *   <li>Triggering scheduled physical deletion after retention period</li>
 *   <li>Updating search indexes to exclude deleted files</li>
 *   <li>Audit logging and compliance tracking</li>
 * </ul>
 *
 * @param fileId          UUID of the deleted file
 * @param filename        Original filename (for audit purposes)
 * @param deletedByUserId UUID of user who deleted the file (null for system deletion)
 * @param deletedAt       Timestamp of deletion
 *
 * @author PoliclicaBine System
 */
public record FileDeleted(
        UUID fileId,
        String filename,
        UUID deletedByUserId,
        LocalDateTime deletedAt
) {
}
