package com.example.policlicabine.service;

import com.example.policlicabine.config.properties.FileStorageProperties;
import com.example.policlicabine.dto.FileDto;
import com.example.policlicabine.dto.UserDto;
import com.example.policlicabine.entity.File;
import com.example.policlicabine.entity.enums.FileCategory;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.FileMapper;
import com.example.policlicabine.repository.FileRepository;
import com.example.policlicabine.service.storage.FileStorageService;
import com.example.policlicabine.service.storage.StorageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
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

    /**
     * Upload a new file
     *
     * @param multipartFile    the file to upload
     * @param category        file category
     * @param username        username of user uploading the file (from JWT token)
     * @param validFrom       start date of validity (optional)
     * @param validUntil      end date of validity (optional)
     * @return FileDto
     */
    public FileDto uploadFile(
            MultipartFile multipartFile,
            FileCategory category,
            String username,
            LocalDate validFrom,
            LocalDate validUntil
    ) {
        // Validate file
        validateFile(multipartFile);

        // Validate dates
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new BusinessException("Valid until date must be after valid from date");
        }

        // Resolve username to User entity
        UserDto userDto = userService.findUserByUsername(username);

        UUID uploadedByUserId = userDto.getUserId();
        User uploader = userService.getEntityById(uploadedByUserId);
        if (uploader == null) {
            throw new ResourceNotFoundException("User", username);
        }

        // Generate unique filename
        String uniqueFilename = generateUniqueFilename(multipartFile.getOriginalFilename());

        // Store file
        StorageResult storageData = fileStorageService.storeFile(
                multipartFile, category, uniqueFilename
        );

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

        return fileMapper.toDto(saved);
    }

    /**
     * Upload a new version of an existing file
     *
     * @param previousFileId   UUID of the file to replace
     * @param multipartFile    the new file
     * @param username        username of user uploading (from JWT token)
     * @return FileDto
     */
    public FileDto uploadNewVersion(
            UUID previousFileId,
            MultipartFile multipartFile,
            String username
    ) {
        // Load previous version
        File previousFile = fileRepository
                .findByIdAndIsDeletedFalse(previousFileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", previousFileId));

        // Upload new version with same category and validity
        FileDto uploadedDto = uploadFile(
                multipartFile,
                previousFile.getFileCategory(),
                username,
                previousFile.getValidFrom(),
                previousFile.getValidUntil()
        );

        // Update version info
        File newVersion = fileRepository.findById(uploadedDto.getId()).orElseThrow();

        newVersion.setVersion(previousFile.getVersion() + 1);
        newVersion.setPreviousVersionId(previousFileId);

        // Resolve username to User entity for soft delete
        UserDto userDto = userService.findUserByUsername(username);

        UUID uploadedByUserId = userDto.getUserId();
        User uploader = userService.getEntityById(uploadedByUserId);
        previousFile.softDelete(uploader);

        fileRepository.saveAll(List.of(newVersion, previousFile));

        log.info("New file version created: {} (v{}) replacing: {}",
                newVersion.getId(), newVersion.getVersion(), previousFileId);

        return fileMapper.toDto(newVersion);
    }

    /**
     * Find file by ID
     *
     * @param fileId the file UUID
     * @return FileDto
     */
    @Transactional(readOnly = true)
    public FileDto findById(UUID fileId) {
        File file = fileRepository
                .findWithUploadedByById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        return fileMapper.toDto(file);
    }

    /**
     * Find all active files by category
     *
     * @param category the file category
     * @return List of FileDtos
     */
    @Transactional(readOnly = true)
    public List<FileDto> findByCategory(FileCategory category) {
        List<File> files = fileRepository
                .findWithUploadedByByFileCategoryAndIsDeletedFalse(category);

        return files.stream()
                .map(fileMapper::toDto)
                .toList();
    }

    /**
     * Get version history for a file
     *
     * @param fileId the file UUID
     * @return List of all versions
     */
    @Transactional(readOnly = true)
    public List<FileDto> getFileVersionHistory(UUID fileId) {
        // Find the file
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

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

        return allVersions.stream()
                .map(fileMapper::toDto)
                .toList();
    }

    /**
     * Download file as Resource
     *
     * @param fileId the file UUID
     * @return Resource
     */
    @Transactional(readOnly = true)
    public Resource downloadFile(UUID fileId) {
        File file = fileRepository
                .findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        if (file.isExpired()) {
            log.warn("Attempt to download expired file: {}", fileId);
            throw new BusinessException("File has expired");
        }

        Resource resource = fileStorageService.loadFile(file.getStoragePath());
        log.debug("File downloaded: {} by user", fileId);

        return resource;
    }

    /**
     * Soft delete a file
     *
     * @param fileId           the file UUID
     * @param username        username of user deleting the file (from JWT token)
     */
    public void softDeleteFile(UUID fileId, String username) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));

        if (file.getIsDeleted()) {
            throw new BusinessException("File already deleted");
        }

        // Resolve username to User entity
        UserDto userDto = userService.findUserByUsername(username);

        UUID deletedByUserId = userDto.getUserId();
        User deletedBy = userService.getEntityById(deletedByUserId);
        file.softDelete(deletedBy);
        fileRepository.save(file);

        log.info("File soft-deleted: {} by user: {}", fileId, username);
    }

    /**
     * Find expired files that are still active
     *
     * @return List of expired files
     */
    @Transactional(readOnly = true)
    public List<FileDto> findExpiredFiles() {
        List<File> expiredFiles = fileRepository
                .findByValidUntilBeforeAndIsDeletedFalse(LocalDate.now());

        return expiredFiles.stream()
                .map(fileMapper::toDto)
                .toList();
    }

    /**
     * Validate uploaded file (type, size, content)
     *
     * @param file the multipart file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty or null");
        }

        // Check file size
        DataSize fileSize = DataSize.ofBytes(file.getSize());
        DataSize maxSize = properties.getMaxFileSize();

        if (fileSize.compareTo(maxSize) > 0) {
            throw new BusinessException(String.format(
                    "File size (%s) exceeds maximum limit of %s",
                    formatFileSize(file.getSize()),
                    formatFileSize(maxSize.toBytes())
            ));
        }

        // Check MIME type
//        String mimeType = file.getContentType();
//        if (mimeType == null || !properties.getAllowedMimeTypes().contains(mimeType)) {
//            throw new BusinessException("File type not allowed: " + mimeType +
//                    ". Allowed types: " + String.join(", ", properties.getAllowedMimeTypes()));
//        }

        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new BusinessException("Filename is required");
        }
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
