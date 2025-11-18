package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.config.properties.FileStorageProperties;
import com.example.policlicabine.dto.FileDto;
import com.example.policlicabine.entity.File;
import com.example.policlicabine.entity.FileCategory;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.FileDeleted;
import com.example.policlicabine.event.FileUploaded;
import com.example.policlicabine.event.FileVersionCreated;
import com.example.policlicabine.mapper.FileMapper;
import com.example.policlicabine.repository.FileRepository;
import com.example.policlicabine.service.storage.FileStorageService;
import com.example.policlicabine.service.storage.StorageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for file management operations.
 *
 * <p>Provides business logic for:
 * <ul>
 *   <li>File upload with validation and metadata tracking</li>
 *   <li>File download with access control</li>
 *   <li>Version management and history tracking</li>
 *   <li>Soft delete with audit trail</li>
 *   <li>File expiration management</li>
 * </ul>
 *
 * @author PoliclicaBine System
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final FileMapper fileMapper;
    private final UserService userService;
    private final FileStorageProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Upload a new file
     *
     * @param multipartFile    the file to upload
     * @param category        file category
     * @param username        username of user uploading the file (from JWT token)
     * @param validFrom       start date of validity (optional)
     * @param validUntil      end date of validity (optional)
     * @return Result containing FileDto or error message
     */
    public Result<FileDto> uploadFile(
            MultipartFile multipartFile,
            FileCategory category,
            String username,
            LocalDate validFrom,
            LocalDate validUntil
    ) {
        // Validate file
        Result<Void> validation = validateFile(multipartFile);
        if (validation.isFailure()) {
            return Result.failure(validation.getErrorMessage());
        }

        // Validate dates
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            return Result.failure("Valid until date must be after valid from date");
        }

        // Resolve username to User entity
        Result<com.example.policlicabine.dto.UserDto> userResult = userService.findUserByUsername(username);
        if (userResult.isFailure()) {
            return Result.failure("Authenticated user not found: " + username);
        }

        UUID uploadedByUserId = userResult.getValue().getUserId();
        User uploader = userService.getEntityById(uploadedByUserId);
        if (uploader == null) {
            return Result.failure("User not found: " + username);
        }

        // Generate unique filename
        String uniqueFilename = generateUniqueFilename(multipartFile.getOriginalFilename());

        // Store file
        Result<StorageResult> storeResult = fileStorageService.storeFile(
                multipartFile, category, uniqueFilename
        );

        if (storeResult.isFailure()) {
            return Result.failure(storeResult.getErrorMessage());
        }

        StorageResult storageData = storeResult.getValue();

        // Create File entity
        File file = File.builder()
                .originalFilename(multipartFile.getOriginalFilename())
                .storedFilename(storageData.getStoredFilename())
                .storagePath(storageData.getStoragePath())
                .fileSize(storageData.getFileSize())
                .mimeType(multipartFile.getContentType())
                .checksum(storageData.getChecksum())
                .fileCategory(category)
                .uploadedBy(uploader)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .version(1)
                .build();

        File saved = fileRepository.save(file);

        log.info("File uploaded successfully: {} by user: {}", saved.getId(), uploadedByUserId);

        // Publish event AFTER save, BEFORE returning
        eventPublisher.publishEvent(new FileUploaded(
                saved.getId(),
                saved.getOriginalFilename(),
                category,
                uploadedByUserId,
                saved.getFileSize()
        ));

        return Result.success(fileMapper.toDto(saved));
    }

    /**
     * Upload a new version of an existing file
     *
     * @param previousFileId   UUID of the file to replace
     * @param multipartFile    the new file
     * @param username        username of user uploading (from JWT token)
     * @return Result containing new FileDto or error message
     */
    public Result<FileDto> uploadNewVersion(
            UUID previousFileId,
            MultipartFile multipartFile,
            String username
    ) {
        // Load previous version
        File previousFile = fileRepository
                .findByIdAndIsDeletedFalse(previousFileId)
                .orElse(null);

        if (previousFile == null) {
            return Result.failure("Previous file version not found: " + previousFileId);
        }

        // Upload new version with same category and validity
        Result<FileDto> uploadResult = uploadFile(
                multipartFile,
                previousFile.getFileCategory(),
                username,
                previousFile.getValidFrom(),
                previousFile.getValidUntil()
        );

        if (uploadResult.isFailure()) {
            return uploadResult;
        }

        // Update version info
        File newVersion = fileRepository.findById(
                uploadResult.getValue().getId()
        ).orElseThrow();

        newVersion.setVersion(previousFile.getVersion() + 1);
        newVersion.setPreviousVersionId(previousFileId);

        // Resolve username to User entity for soft delete
        Result<com.example.policlicabine.dto.UserDto> userResult = userService.findUserByUsername(username);
        if (userResult.isFailure()) {
            return Result.failure("Authenticated user not found: " + username);
        }

        UUID uploadedByUserId = userResult.getValue().getUserId();
        User uploader = userService.getEntityById(uploadedByUserId);
        previousFile.softDelete(uploader);

        fileRepository.saveAll(List.of(newVersion, previousFile));

        log.info("New file version created: {} (v{}) replacing: {}",
                newVersion.getId(), newVersion.getVersion(), previousFileId);

        // Publish event
        eventPublisher.publishEvent(new FileVersionCreated(
                newVersion.getId(),
                previousFileId,
                newVersion.getVersion(),
                uploadedByUserId
        ));

        return Result.success(fileMapper.toDto(newVersion));
    }

    /**
     * Find file by ID
     *
     * @param fileId the file UUID
     * @return Result containing FileDto or error message
     */
    @Transactional(readOnly = true)
    public Result<FileDto> findById(UUID fileId) {
        File file = fileRepository
                .findWithUploadedByById(fileId)
                .orElse(null);

        if (file == null) {
            return Result.failure("File not found: " + fileId);
        }

        return Result.success(fileMapper.toDto(file));
    }

    /**
     * Find all active files by category
     *
     * @param category the file category
     * @return Result containing list of FileDtos
     */
    @Transactional(readOnly = true)
    public Result<List<FileDto>> findByCategory(FileCategory category) {
        List<File> files = fileRepository
                .findWithUploadedByByFileCategoryAndIsDeletedFalse(category);

        List<FileDto> dtos = files.stream()
                .map(fileMapper::toDto)
                .toList();

        return Result.success(dtos);
    }

    /**
     * Get version history for a file
     *
     * @param fileId the file UUID
     * @return Result containing list of all versions
     */
    @Transactional(readOnly = true)
    public Result<List<FileDto>> getFileVersionHistory(UUID fileId) {
        // Find the file
        File file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return Result.failure("File not found: " + fileId);
        }

        // Find all newer versions
        List<File> newerVersions = fileRepository.findByPreviousVersionId(fileId);

        // Find all older versions by traversing previousVersionId chain
        List<File> allVersions = new java.util.ArrayList<>();
        allVersions.add(file);
        allVersions.addAll(newerVersions);

        // Traverse backwards to find older versions
        UUID currentPreviousId = file.getPreviousVersionId();
        while (currentPreviousId != null) {
            File previousVersion = fileRepository.findById(currentPreviousId).orElse(null);
            if (previousVersion != null) {
                allVersions.add(previousVersion);
                currentPreviousId = previousVersion.getPreviousVersionId();
            } else {
                break;
            }
        }

        // Sort by version number
        allVersions.sort((f1, f2) -> f2.getVersion().compareTo(f1.getVersion()));

        List<FileDto> dtos = allVersions.stream()
                .map(fileMapper::toDto)
                .toList();

        return Result.success(dtos);
    }

    /**
     * Download file as Resource
     *
     * @param fileId the file UUID
     * @return Result containing Resource or error message
     */
    @Transactional(readOnly = true)
    public Result<Resource> downloadFile(UUID fileId) {
        File file = fileRepository
                .findByIdAndIsDeletedFalse(fileId)
                .orElse(null);

        if (file == null) {
            return Result.failure("File not found or deleted: " + fileId);
        }

        if (file.isExpired()) {
            log.warn("Attempt to download expired file: {}", fileId);
            return Result.failure("File has expired");
        }

        Result<Resource> loadResult = fileStorageService.loadFile(file.getStoragePath());

        if (loadResult.isSuccess()) {
            log.debug("File downloaded: {} by user", fileId);
        }

        return loadResult;
    }

    /**
     * Soft delete a file
     *
     * @param fileId           the file UUID
     * @param username        username of user deleting the file (from JWT token)
     * @return Result success or failure
     */
    public Result<Void> softDeleteFile(UUID fileId, String username) {
        File file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return Result.failure("File not found: " + fileId);
        }

        if (file.getIsDeleted()) {
            return Result.failure("File already deleted");
        }

        // Resolve username to User entity
        Result<com.example.policlicabine.dto.UserDto> userResult = userService.findUserByUsername(username);
        if (userResult.isFailure()) {
            return Result.failure("Authenticated user not found: " + username);
        }

        UUID deletedByUserId = userResult.getValue().getUserId();
        User deletedBy = userService.getEntityById(deletedByUserId);
        file.softDelete(deletedBy);
        fileRepository.save(file);

        log.info("File soft-deleted: {} by user: {}", fileId, username);

        // Publish event
        eventPublisher.publishEvent(new FileDeleted(
                fileId,
                file.getOriginalFilename(),
                deletedByUserId,
                LocalDateTime.now()
        ));

        return Result.success(null);
    }

    /**
     * Find expired files that are still active
     *
     * @return Result containing list of expired files
     */
    @Transactional(readOnly = true)
    public Result<List<FileDto>> findExpiredFiles() {
        List<File> expiredFiles = fileRepository
                .findByValidUntilBeforeAndIsDeletedFalse(LocalDate.now());

        List<FileDto> dtos = expiredFiles.stream()
                .map(fileMapper::toDto)
                .toList();

        return Result.success(dtos);
    }

    /**
     * Validate uploaded file (type, size, content)
     *
     * @param file the multipart file
     * @return Result success or failure with message
     */
    private Result<Void> validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.failure("File is empty or null");
        }

        // Check file size
        DataSize fileSize = DataSize.ofBytes(file.getSize());
        DataSize maxSize = properties.getMaxFileSize();

        if (fileSize.compareTo(maxSize) > 0) {
            return Result.failure(String.format(
                    "File size (%s) exceeds maximum limit of %s",
                    formatFileSize(file.getSize()),
                    formatFileSize(maxSize.toBytes())
            ));
        }

        // Check MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || !properties.getAllowedMimeTypes().contains(mimeType)) {
            return Result.failure("File type not allowed: " + mimeType +
                    ". Allowed types: " + String.join(", ", properties.getAllowedMimeTypes()));
        }

        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            return Result.failure("Filename is required");
        }

        return Result.success(null);
    }

    /**
     * Generate unique filename with timestamp and UUID
     *
     * @param originalFilename the original filename
     * @return unique filename
     */
    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = FilenameUtils.getExtension(originalFilename);

        if (extension == null || extension.isEmpty()) {
            return String.format("%s_%s", timestamp, uuid);
        }

        return String.format("%s_%s.%s", timestamp, uuid, extension);
    }

    /**
     * Format file size to human-readable string
     *
     * @param bytes file size in bytes
     * @return formatted string like "1.5 MB"
     */
    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }

        double size = bytes / Math.pow(1024, digitGroups);
        return String.format("%.1f %s", size, units[digitGroups]);
    }
}
