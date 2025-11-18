package com.example.policlicabine.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing uploaded files with comprehensive metadata tracking.
 *
 * <p>Features:
 * <ul>
 *   <li>UUID v7 primary keys (time-sorted for better database performance)</li>
 *   <li>Full metadata tracking (filename, size, MIME type, checksum)</li>
 *   <li>Validity period support (expiring documents like consent forms)</li>
 *   <li>Version history tracking (previousVersionId chain)</li>
 *   <li>Soft delete with audit trail</li>
 *   <li>Upload and deletion user tracking</li>
 * </ul>
 *
 * @author PoliclicaBine System
 */
@Entity
@Table(name = "files", indexes = {
    @Index(name = "idx_file_category", columnList = "file_category"),
    @Index(name = "idx_file_uploaded_at", columnList = "uploaded_at"),
    @Index(name = "idx_file_valid_until", columnList = "valid_until"),
    @Index(name = "idx_file_previous_version", columnList = "previous_version_id"),
    @Index(name = "idx_file_is_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class File {

    /**
     * Primary key - UUID v7 (time-sorted for better DB index performance)
     */
    @Id
    @Column(name = "file_id", updatable = false, nullable = false)
    UUID id;

    // ==================== Basic File Metadata ====================

    /**
     * Original filename as provided by the user during upload
     */
    @Column(name = "original_filename", nullable = false, length = 255)
    String originalFilename;

    /**
     * Unique filename used for storage (prevents collisions and security issues)
     */
    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    String storedFilename;

    /**
     * Relative path in storage system (e.g., "consent_file/2024/01/filename.pdf")
     */
    @Column(name = "storage_path", nullable = false, length = 500)
    String storagePath;

    /**
     * File size in bytes
     */
    @Column(name = "file_size", nullable = false)
    Long fileSize;

    /**
     * MIME type (e.g., "image/jpeg", "application/pdf")
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    String mimeType;

    /**
     * SHA-256 checksum for file integrity verification
     */
    @Column(name = "checksum", nullable = false, length = 64)
    String checksum;

    // ==================== Categorization ====================

    /**
     * Category classification for the file
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", nullable = false, length = 50)
    FileCategory fileCategory;

    // ==================== Upload Metadata ====================

    /**
     * User who uploaded the file
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    User uploadedBy;

    /**
     * Timestamp when the file was uploaded (automatically set by Hibernate)
     */
    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    LocalDateTime uploadedAt;

    // ==================== Validity Period ====================

    /**
     * Start date of file validity (optional)
     */
    @Column(name = "valid_from")
    LocalDate validFrom;

    /**
     * End date of file validity (e.g., consent forms expire after 1 year)
     */
    @Column(name = "valid_until")
    LocalDate validUntil;

    // ==================== Soft Delete (Audit Trail) ====================

    /**
     * Soft delete flag (files are never hard-deleted for audit purposes)
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    Boolean isDeleted = false;

    /**
     * Timestamp when the file was soft-deleted
     */
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    /**
     * User who soft-deleted the file
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    User deletedBy;

    // ==================== Version Management ====================

    /**
     * Version number (starts at 1, increments with each upload)
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    Integer version = 1;

    /**
     * Reference to the previous version of this file (for version history chain)
     */
    @Column(name = "previous_version_id")
    UUID previousVersionId;

    // ==================== JPA Lifecycle Callbacks ====================

    /**
     * Generate UUID v7 for new files before persisting to database.
     * Uses time-ordered UUID v7 for better database index performance.
     */
    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UuidCreator.getTimeOrderedEpoch();
            log.debug("Generated UUID v7: {} for new file entity", id);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Check if the file has expired based on validUntil date
     *
     * @return true if file has expired, false otherwise
     */
    public boolean isExpired() {
        return validUntil != null && LocalDate.now().isAfter(validUntil);
    }

    /**
     * Perform soft delete operation (sets deletion metadata)
     *
     * @param deletedByUser the user performing the deletion
     */
    public void softDelete(User deletedByUser) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUser;
        log.info("File soft-deleted: {} by user: {}", 
                this.id,
                deletedByUser != null ? deletedByUser.getUserId() : "SYSTEM");
    }

    /**
     * Check if file is still active (not deleted and not expired)
     *
     * @return true if file is active
     */
    public boolean isActive() {
        return !isDeleted && !isExpired();
    }

    // ==================== JPA equals/hashCode ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof File)) return false;
        File file = (File) o;
        return id != null && Objects.equals(id, file.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "File{" +
                "id=" + id +
                ", filename='" + originalFilename + '\'' +
                ", category=" + fileCategory +
                ", size=" + fileSize +
                ", version=" + version +
                ", deleted=" + isDeleted +
                '}';
    }
}
