package com.example.policlicabine.event;

import java.util.UUID;

/**
 * Domain event published when a new version of an existing file is uploaded.
 *
 * <p>This event is published after the new file version is stored and linked
 * to the previous version via previousVersionId. Event listeners can use this for:
 * <ul>
 *   <li>Notifying users about document updates</li>
 *   <li>Triggering version comparison workflows</li>
 *   <li>Managing version history cleanup policies</li>
 *   <li>Updating references to use the latest version</li>
 * </ul>
 *
 * @param newFileId          UUID of the new file version
 * @param previousFileId     UUID of the previous file version
 * @param version            Version number of the new file
 * @param uploadedByUserId   UUID of user who uploaded the new version
 *
 * @author PoliclicaBine System
 */
public record FileVersionCreated(
        UUID newFileId,
        UUID previousFileId,
        Integer version,
        UUID uploadedByUserId
) {
}
