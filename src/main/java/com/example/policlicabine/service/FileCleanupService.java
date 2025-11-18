package com.example.policlicabine.service;

import com.example.policlicabine.entity.File;
import com.example.policlicabine.repository.FileRepository;
import com.example.policlicabine.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for automated file cleanup tasks.
 *
 * <p>Scheduled tasks:
 * <ul>
 *   <li>Daily cleanup of expired files (soft delete)</li>
 *   <li>Weekly cleanup of old soft-deleted files (physical deletion after 90 days)</li>
 * </ul>
 *
 * @author PoliclicaBine System
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileCleanupService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    /**
     * Auto-soft-delete expired files
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredFiles() {
        log.info("Starting cleanup of expired files");

        try {
            LocalDate today = LocalDate.now();
            List<File> expiredFiles = fileRepository
                    .findByValidUntilBeforeAndIsDeletedFalse(today);

            if (expiredFiles.isEmpty()) {
                log.info("No expired files found");
                return;
            }

            expiredFiles.forEach(file -> {
                log.info("Auto-deleting expired file: {} (expired on: {})",
                        file.getOriginalFilename(), file.getValidUntil());
                file.softDelete(null); // System auto-deletion
            });

            fileRepository.saveAll(expiredFiles);
            log.info("Cleanup completed. Soft-deleted {} expired files", expiredFiles.size());

        } catch (Exception e) {
            log.error("Error during expired files cleanup", e);
        }
    }

    /**
     * Permanently delete old soft-deleted files (retention policy: 90 days)
     * Runs weekly on Sunday at 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void cleanupOldDeletedFiles() {
        log.info("Starting cleanup of old deleted files");

        try {
            // Find files deleted more than 90 days ago
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
            List<File> oldDeletedFiles = fileRepository
                    .findByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

            if (oldDeletedFiles.isEmpty()) {
                log.info("No old deleted files found");
                return;
            }

            int successCount = 0;
            int failureCount = 0;

            for (File file : oldDeletedFiles) {
                try {
                    // Delete physical file
                    fileStorageService.deleteFile(file.getStoragePath());

                    // Delete database record
                    fileRepository.delete(file);

                    log.info("Permanently deleted file: {} (deleted on: {})",
                            file.getOriginalFilename(), file.getDeletedAt());
                    successCount++;

                } catch (Exception e) {
                    log.error("Failed to permanently delete file: {}",
                            file.getId(), e);
                    failureCount++;
                }
            }

            log.info("Old file cleanup completed. Success: {}, Failed: {}",
                    successCount, failureCount);

        } catch (Exception e) {
            log.error("Error during old deleted files cleanup", e);
        }
    }

    /**
     * Log file storage statistics
     * Runs daily at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(readOnly = true)
    public void logStorageStatistics() {
        try {
            long totalFiles = fileRepository.count();
            long activeFiles = fileRepository.countByIsDeletedFalse();
            long deletedFiles = totalFiles - activeFiles;

            log.info("=== File Storage Statistics ===");
            log.info("Total files: {}", totalFiles);
            log.info("Active files: {}", activeFiles);
            log.info("Deleted files: {}", deletedFiles);
            log.info("===============================");

        } catch (Exception e) {
            log.error("Error logging storage statistics", e);
        }
    }
}
