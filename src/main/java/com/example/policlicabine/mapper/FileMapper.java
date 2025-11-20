package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.FileDto;
import com.example.policlicabine.entity.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.text.DecimalFormat;

/**
 * MapStruct mapper for converting between File entity and FileDto.
 *
 * <p>Provides type-safe, compile-time mapping with custom methods for
 * computed fields like downloadUrl, isExpired, and formatted file size.
 *
 * @author PoliclicaBine System
 */
@Mapper(componentModel = "spring")
public interface FileMapper {

    /**
     * Convert File entity to FileDto with computed fields
     *
     * @param file the File entity
     * @return FileDto with all fields populated
     */
    @Mapping(target = "downloadUrl", expression = "java(buildDownloadUrl(file))")
    @Mapping(target = "isExpired", expression = "java(file.isExpired())")
    @Mapping(target = "isActive", expression = "java(file.isActive())")
    @Mapping(target = "fileSizeFormatted", expression = "java(formatFileSize(file.getFileSize()))")
    @Mapping(target = "patientId", expression = "java(file.getPatient() != null ? file.getPatient().getPatientId() : null)")
    FileDto toDto(File file);

    /**
     * Convert FileDto to File entity (for reverse mapping)
     * Ignores patient field as it's a bidirectional relationship managed by PatientMapper
     *
     * @param dto the FileDto
     * @return File entity
     */
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    File toEntity(FileDto dto);

    /**
     * Build download URL for a file
     *
     * @param file the File entity
     * @return download URL path
     */
    default String buildDownloadUrl(File file) {
        if (file == null || file.getId() == null) {
            return null;
        }
        return "/api/files/" + file.getId() + "/download";
    }

    /**
     * Format file size into human-readable string (KB, MB, GB)
     *
     * @param bytes file size in bytes
     * @return formatted string like "1.5 MB"
     */
    default String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }

        double size = bytes / Math.pow(1024, digitGroups);
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");

        return decimalFormat.format(size) + " " + units[digitGroups];
    }
}
