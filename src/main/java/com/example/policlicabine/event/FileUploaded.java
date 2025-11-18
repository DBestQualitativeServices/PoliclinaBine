package com.example.policlicabine.event;

import com.example.policlicabine.entity.FileCategory;

import java.util.UUID;

/**
 * Domain event published when a file is successfully uploaded.
 *
 * <p>This event is published after the file is stored and the File entity
 * is persisted to the database. Event listeners can use this for:
 * <ul>
 *   <li>Sending notifications to users</li>
 *   <li>Triggering virus scanning</li>
 *   <li>Generating thumbnails for images</li>
 *   <li>Indexing file metadata for search</li>
 *   <li>Logging and audit trails</li>
 * </ul>
 *
 * @param fileId          UUID of the uploaded file
 * @param filename        Original filename
 * @param category        File category classification
 * @param uploadedByUserId UUID of user who uploaded the file
 * @param fileSize        File size in bytes
 *
 * @author PoliclicaBine System
 */
public record FileUploaded(
        UUID fileId,
        String filename,
        FileCategory category,
        UUID uploadedByUserId,
        Long fileSize
) {
}
