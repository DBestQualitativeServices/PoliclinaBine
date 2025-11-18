package com.example.policlicabine.repository;

import com.example.policlicabine.entity.File;
import com.example.policlicabine.entity.FileCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for File entity data access operations.
 *
 * <p>Provides efficient queries with @EntityGraph to prevent N+1 query issues
 * when loading files with their relationships (uploadedBy, deletedBy users).
 *
 * @author PoliclicaBine System
 */
@Repository
public interface FileRepository extends JpaRepository<File, UUID> {

    /**
     * Find active (non-deleted) file by ID
     *
     * @param fileId the file UUID
     * @return Optional containing file if found and not deleted
     */
    Optional<File> findByIdAndIsDeletedFalse(UUID fileId);

    /**
     * Find file by ID with uploadedBy user loaded (prevents N+1 query)
     *
     * @param id the file UUID
     * @return Optional containing file with uploadedBy relationship loaded
     */
    @EntityGraph(attributePaths = {"uploadedBy"})
    @Query("SELECT f FROM File f WHERE f.id = :id")
    Optional<File> findWithUploadedByById(@Param("id") UUID id);

    /**
     * Find file by ID with all relationships loaded (for DTO mapping)
     *
     * @param id the file UUID
     * @return Optional containing file with all relationships
     */
    @EntityGraph(attributePaths = {"uploadedBy", "deletedBy"})
    @Query("SELECT f FROM File f WHERE f.id = :id")
    Optional<File> findWithAllRelationshipsById(@Param("id") UUID id);

    /**
     * Find all active files by category
     *
     * @param category the file category
     * @return List of active files in the category
     */
    List<File> findByFileCategoryAndIsDeletedFalse(FileCategory category);

    /**
     * Find all active files by category with uploadedBy user loaded
     *
     * @param category the file category
     * @return List of files with relationships loaded
     */
    @EntityGraph(attributePaths = {"uploadedBy"})
    @Query("SELECT f FROM File f WHERE f.fileCategory = :category AND f.isDeleted = false")
    List<File> findWithUploadedByByFileCategoryAndIsDeletedFalse(@Param("category") FileCategory category);

    /**
     * Find newer versions of a file (files with previousVersionId)
     *
     * @param previousVersionId UUID of the previous version
     * @return List of files that are newer versions
     */
    List<File> findByPreviousVersionId(UUID previousVersionId);

    /**
     * Find files uploaded by a specific user
     *
     * @param userId the user UUID
     * @return List of files uploaded by the user
     */
    List<File> findByUploadedByUserIdAndIsDeletedFalse(UUID userId);

    /**
     * Find expired files (validUntil date has passed)
     *
     * @param date the date to compare against (typically today)
     * @return List of expired but not deleted files
     */
    List<File> findByValidUntilBeforeAndIsDeletedFalse(LocalDate date);

    /**
     * Find files expiring within a date range (for alerts)
     *
     * @param startDate start of the range
     * @param endDate   end of the range
     * @return List of files expiring within the range
     */
    List<File> findByValidUntilBetweenAndIsDeletedFalse(LocalDate startDate, LocalDate endDate);

    /**
     * Find all deleted files (for cleanup/archive operations)
     *
     * @return List of soft-deleted files
     */
    List<File> findByIsDeletedTrue();

    /**
     * Find deleted files older than a certain date (for permanent deletion)
     *
     * @param date the cutoff date
     * @return List of files deleted before the date
     */
    List<File> findByIsDeletedTrueAndDeletedAtBefore(java.time.LocalDateTime date);

    /**
     * Check if a file with the stored filename already exists
     *
     * @param storedFilename the unique stored filename
     * @return true if exists, false otherwise
     */
    boolean existsByStoredFilename(String storedFilename);

    /**
     * Count active files by category
     *
     * @param category the file category
     * @return count of active files
     */
    long countByFileCategoryAndIsDeletedFalse(FileCategory category);

    /**
     * Count total active files
     *
     * @return count of all active files
     */
    long countByIsDeletedFalse();
}
